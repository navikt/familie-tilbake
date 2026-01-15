package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil.Companion.TITTEL_VARSEL_TILBAKEBETALING
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.Dokumentvariant
import no.nav.tilbakekreving.pdf.PdfGenerator
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokprodTilHtml
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Period
import java.util.UUID

@Service
class ForhåndsvarselService(
    private val pdfBrevService: PdfBrevService,
    private val varselbrevUtil: VarselbrevUtil,
    private val dokarkivClient: DokarkivClient,
    private val dokdistClient: DokdistClient,
    private val pdfFactory: () -> PdfGenerator = { PdfGenerator() },
) {
    private val logger = TracedLogger.getLogger<ForhåndsvarselService>()

    fun hentVarselbrevTekster(tilbakekreving: Tilbakekreving): Varselbrevtekst {
        val brevmetadata = opprettMetadata(tilbakekreving.hentVarselbrevInfo())
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false)
        val brevbody = TekstformatererVarselbrev.lagFritekst(opprettVarselbrevsdokument(tilbakekreving.hentVarselbrevInfo(), brevmetadata), false)

        val varselbrevAvsnitter = VarselbrevParser.parse(brevbody)
        val varselbrevtekst = Varselbrevtekst(overskrift = overskrift, avsnitter = varselbrevAvsnitter)
        return varselbrevtekst
    }

    fun forhåndsvisVarselbrev(
        tilbakekreving: Tilbakekreving,
        bestillBrevDto: BestillBrevDto,
    ): ByteArray {
        val varselbrevInfo = tilbakekreving.hentVarselbrevInfo()
        val varselbrevsdokument = opprettVarselbrevsdokument(
            varselbrevInfo = varselbrevInfo,
            brevmetadata = opprettMetadata(varselbrevInfo),
        ).copy(varseltekstFraSaksbehandler = bestillBrevDto.fritekst)

        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = Brevmottager.BRUKER,
                overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false),
                brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false),
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = varselbrevUtil.lagVedlegg(varselbrevsdokument, bestillBrevDto.behandlingId),
            ),
        )
    }

    fun bestillVarselbrev(
        tilbakekreving: Tilbakekreving,
        bestillBrevDto: BestillBrevDto,
    ) {
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val sendtTid = LocalDate.now()
        val varselbrevBehov = tilbakekreving.opprettVarselbrevBehov(bestillBrevDto.fritekst)
        val journalpost = journalførVarselbrev(
            varselbrevBehov = varselbrevBehov,
            fristForUttalelse = sendtTid.plus(Period.ofWeeks(3)),
            logContext = logContext,
        )
        if (journalpost.journalpostId == null) {
            throw Feil(
                message = "journalførin av varselbrev til behandlingId ${varselbrevBehov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                frontendFeilmelding = "journalførin av varselbrev til behandlingId ${varselbrevBehov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                logContext = SecureLog.Context.fra(tilbakekreving),
            )
        }
        dokdistClient.brevTilUtsending(
            behandlingId = varselbrevBehov.behandlingId,
            journalpostId = journalpost.journalpostId,
            fagsystem = varselbrevBehov.ytelse.tilFagsystemDTO(),
            distribusjonstype = Distribusjonstype.VIKTIG,
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
            adresse = null,
            logContext = logContext,
        )
        tilbakekreving.oppdaterSendtVarselbrev(
            journalpostId = journalpost.journalpostId,
            varselbrevId = varselbrevBehov.varselbrev.id,
            tekstFraSaksbehandler = varselbrevBehov.varseltekstFraSaksbehandler,
            sendtTid = sendtTid,
            fristForUttalelse = sendtTid.plus(Period.ofWeeks(3)),
        )
    }

    fun lagreUttalelse(tilbakekreving: Tilbakekreving, brukeruttalelse: BrukeruttalelseDto) {
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        when (brukeruttalelse.harBrukerUttaltSeg) {
            HarBrukerUttaltSeg.JA, HarBrukerUttaltSeg.ALLEREDE_UTTALET_SEG -> {
                val uttalelsedetaljer = requireNotNull(brukeruttalelse.uttalelsesdetaljer) {
                    "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null"
                }.also {
                    require(it.isNotEmpty()) {
                        "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var tom"
                    }
                }
                behandling.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = uttalelsedetaljer.map { UttalelseInfo(UUID.randomUUID(), it.uttalelsesdato, it.hvorBrukerenUttalteSeg, it.uttalelseBeskrivelse) },
                    kommentar = null,
                )
            }
            HarBrukerUttaltSeg.NEI -> {
                val kommentar = requireNotNull(brukeruttalelse.kommentar) {
                    "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var null"
                }.also {
                    require(it.isNotBlank()) { "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var tom" }
                }

                behandling.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = listOf(),
                    kommentar = kommentar,
                )
            }
        }
    }

    fun hentForhåndsvarselinfo(tilbakekreving: Tilbakekreving): ForhåndsvarselDto {
        return tilbakekreving.hentForhåndsvarselFrontendDto()
    }

    fun utsettUttalelseFrist(
        tilbakekreving: Tilbakekreving,
        fristUtsettelseDto: FristUtsettelseDto,
    ) {
        requireNotNull(tilbakekreving.brevHistorikk.sisteVarselbrev()) {
            "Kan ikke utsette frist når forhåndsvarsel ikke er sendt"
        }
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        behandling.lagreFristUtsettelse(fristUtsettelseDto.nyFrist, fristUtsettelseDto.begrunnelse)
    }

    fun håndterForhåndsvarselUnntak(
        tilbakekreving: Tilbakekreving,
        forhåndsvarselUnntakDto: ForhåndsvarselUnntakDto,
    ) {
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        behandling.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = when (forhåndsvarselUnntakDto.begrunnelseForUnntak) {
                VarslingsUnntak.IKKE_PRAKTISK_MULIG -> BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG
                VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                VarslingsUnntak.ÅPENBART_UNØDVENDIG -> BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG
            },
            beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
        )
    }

    private fun opprettMetadata(varselbrevInfo: VarselbrevInfo): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevInfo.brukerinfo.ident,
            sakspartsnavn = varselbrevInfo.brukerinfo.navn,
            mottageradresse = Adresseinfo(varselbrevInfo.brukerinfo.ident, varselbrevInfo.brukerinfo.navn),
            behandlendeEnhetsNavn = varselbrevInfo.forhåndsvarselinfo.behandlendeEnhet?.navn ?: "Ukjent", // Todo Fjern ukjent når enhet er på plass,
            ansvarligSaksbehandler = varselbrevInfo.forhåndsvarselinfo.ansvarligSaksbehandler.ident,
            saksnummer = varselbrevInfo.eksternFagsakId,
            språkkode = varselbrevInfo.brukerinfo.språkkode,
            ytelsestype = varselbrevInfo.ytelseType,
            gjelderDødsfall = varselbrevInfo.brukerinfo.dødsdato != null,
        )
    }

    private fun opprettVarselbrevsdokument(varselbrevInfo: VarselbrevInfo, brevmetadata: Brevmetadata): Varselbrevsdokument {
        return Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = varselbrevInfo.forhåndsvarselinfo.beløp,
            revurderingsvedtaksdato = varselbrevInfo.forhåndsvarselinfo.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            feilutbetaltePerioder = varselbrevInfo.forhåndsvarselinfo.feilutbetaltePerioder,
            varsletBeløp = varselbrevInfo.forhåndsvarselinfo.beløp,
            varsletDato = LocalDate.now(),
        )
    }

    fun journalførVarselbrev(
        varselbrevBehov: VarselbrevBehov,
        fristForUttalelse: LocalDate,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse {
        val brevdata = hentBrevdata(varselbrevBehov, fristForUttalelse, logContext)
        val dokument = Dokument(
            dokument = hentPdf(brevdata, logContext),
            filtype = Filtype.PDFA,
            filnavn = "brev.pdf",
            tittel = brevdata.tittel,
        )

        val arkiverDokument =
            ArkiverDokumentRequest(
                fnr = varselbrevBehov.brukerinfo.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                fagsakId = varselbrevBehov.eksternFagsakId,
                journalførendeEnhet = varselbrevBehov.behandlendeEnhet!!.kode,
                avsenderMottaker = AvsenderMottaker(
                    id = varselbrevBehov.brukerinfo.ident,
                    idType = AvsenderMottakerIdType.FNR,
                    navn = varselbrevBehov.brukerinfo.navn,
                ),
                eksternReferanseId = lagEksternReferanseId(
                    varselbrevBehov.behandlingId,
                    Brevtype.VARSEL,
                    brevdata.mottager,
                ),
            )

        return dokarkivClient.opprettOgSendJournalpostRequest(
            arkiverDokument = arkiverDokument,
            fagsaksystem = varselbrevBehov.ytelse.tilDokarkivFagsaksystem(),
            brevkode = varselbrevBehov.ytelse.tilFagsystemDTO().name + "-TILB",
            tema = varselbrevBehov.ytelse.tilTema(),
            dokuemntkategori = DokumentKlasse.B,
            behandlingId = varselbrevBehov.behandlingId,
        )
    }

    private fun hentBrevdata(
        varselbrevBehov: VarselbrevBehov,
        fristForUttalelse: LocalDate,
        logContext: SecureLog.Context,
    ): Brevdata {
        val brevmetadata = hentBrevMetadata(varselbrevBehov)
        val varselbrevsdokument = Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = varselbrevBehov.feilutbetaltBeløp,
            revurderingsvedtaksdato = varselbrevBehov.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = fristForUttalelse,
            varseltekstFraSaksbehandler = varselbrevBehov.varseltekstFraSaksbehandler,
            feilutbetaltePerioder = varselbrevBehov.feilutbetaltePerioder,
        )

        return Brevdata(
            mottager = Brevmottager.BRUKER,
            metadata = brevmetadata,
            tittel = brevmetadata.tittel,
            overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false),
            brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false),
            vedleggHtml = hentVedlegg(varselbrevsdokument, varselbrevBehov.eksternFagsakId, logContext),
        )
    }

    private fun hentBrevMetadata(varselbrevBehov: VarselbrevBehov): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevBehov.brukerinfo.ident,
            sakspartsnavn = varselbrevBehov.brukerinfo.navn,
            tittel = hentVarselbrevTittel(varselbrevBehov),
            mottageradresse = Adresseinfo(varselbrevBehov.brukerinfo.ident, varselbrevBehov.brukerinfo.navn),
            behandlendeEnhetsNavn = requireNotNull(varselbrevBehov.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = requireNotNull(varselbrevBehov.varselbrev.ansvarligSaksbehandlerIdent) { "ansvarligSaksbehandlerIdent kreves for journalføring" },
            saksnummer = varselbrevBehov.eksternFagsakId,
            språkkode = varselbrevBehov.brukerinfo.språkkode,
            ytelsestype = varselbrevBehov.ytelse.tilYtelseDTO(),
            gjelderDødsfall = varselbrevBehov.gjelderDødsfall,
            institusjon = null, // Todo når vi skal håndtere institusjon
        )
    }

    private fun hentVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        eksternFagsakId: String,
        logContext: SecureLog.Context,
    ): String {
        return varselbrevUtil.lagVedlegg(
            varselbrevsdokument,
            eksternFagsakId,
            varselbrevsdokument.beløp,
            logContext,
        )
    }

    private fun hentPdf(brevdata: Brevdata, logContext: SecureLog.Context): ByteArray {
        val pdfGenerator = pdfFactory()
        val html = lagHtml(brevdata)
        try {
            return pdfGenerator.genererPDFMedLogo(
                html = html,
                dokumentvariant = Dokumentvariant.ENDELIG,
                dokumenttittel = brevdata.overskrift,
            )
        } catch (e: Exception) {
            logger.medContext(logContext) {
                info("Feil ved generering av brev: brevData=$brevdata, html=$html", e)
            }
            throw e
        }
    }

    private fun lagHtml(data: Brevdata): String {
        val header = TekstformatererHeader.lagHeader(
            brevmetadata = data.metadata,
            overskrift = data.overskrift,
        )
        val innholdHtml = DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagEksternReferanseId(
        behandlingId: UUID,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        // alle brev kan potensielt bli sendt til både bruker og kopi verge. 2 av breva kan potensielt bli sendt flere gonger
        val callId = callId()
        return "${behandlingId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }

    private fun hentVarselbrevTittel(varselbrevBehov: VarselbrevBehov): String {
        return "$TITTEL_VARSEL_TILBAKEBETALING ${varselbrevBehov.ytelse.hentYtelsesnavn(Språkkode.NB)}"
    }
}
