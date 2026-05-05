package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil.Companion.TITTEL_VARSEL_TILBAKEBETALING
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
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
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UpdateUttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsUnntakDto
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
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ForhåndsvarselService(
    private val pdfBrevService: PdfBrevService,
    private val varselbrevUtil: VarselbrevUtil,
    private val dokarkivClient: DokarkivClient,
    private val dokdistClient: DokdistClient,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
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
        tilbakekreving.trengerVarselbrev(bestillBrevDto.fritekst)
    }

    fun lagreUttalelse(tilbakekreving: Tilbakekreving, brukeruttalelse: BrukeruttalelseDto, behandler: Behandler) {
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        when (brukeruttalelse.harBrukerUttaltSeg) {
            HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL, HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALT_SEG -> {
                val uttalelsedetaljer = requireNotNull(brukeruttalelse.uttalelsesdetaljer) {
                    "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null"
                }.also {
                    require(it.isNotEmpty()) {
                        "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var tom"
                    }
                }[0]
                tilbakekreving.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = UttalelseInfo(UUID.randomUUID(), uttalelsedetaljer.uttalelsesdato, uttalelsedetaljer.hvorBrukerenUttalteSeg, uttalelsedetaljer.uttalelseBeskrivelse),
                    kommentar = null,
                    behandler = behandler,
                )
            }
            HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL, HarBrukerUttaltSeg.UNNTAK_INGEN_UTTALELSE -> {
                val kommentar = requireNotNull(brukeruttalelse.kommentar) {
                    "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var null"
                }.also {
                    require(it.isNotBlank()) { "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var tom" }
                }

                tilbakekreving.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = null,
                    kommentar = kommentar,
                    behandler = behandler,
                )
            }
            else -> throw IllegalArgumentException("Ukjent verdi for uttalelseVurdering: ${brukeruttalelse.harBrukerUttaltSeg} ")
        }
    }

    fun nyLagreUttalelse(tilbakekreving: Tilbakekreving, uttalelseDto: UttalelseDto, behandler: Behandler) {
        val uttalelseVurdering = when (uttalelseDto.harBrukerUttaltSeg) {
            UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL -> UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL
            UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL -> UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL
            UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG -> UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG
            UttalelseVurderingDto.UNNTAK_INGEN_UTTALELSE -> UttalelseVurdering.UNNTAK_INGEN_UTTALELSE
            UttalelseVurderingDto.IKKE_VURDERT -> throw IllegalStateException(
                "Burde ikke være i denne tilstanden. IKKE_VURDERT er enum til frontend.",
            )
        }

        when (uttalelseVurdering) {
            UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG, UttalelseVurdering.JA -> {
                tilbakekreving.lagreUttalelse(
                    uttalelseVurdering = uttalelseVurdering,
                    uttalelseInfo = UttalelseInfo(
                        id = UUID.randomUUID(),
                        uttalelsesdato = requireNotNull(uttalelseDto.uttalelsesdato) { "Det kreves uttalelsesdato når brukeren har uttalet seg. uttalelsesdato var null" },
                        hvorBrukerenUttalteSeg = requireNotNull(uttalelseDto.hvorBrukerenUttalteSeg) {
                            "Det kreves hvorBrukerenUttalteSeg når brukeren har uttalet seg. hvorBrukerenUttalteSeg var null"
                        },
                        uttalelseBeskrivelse = requireNotNull(uttalelseDto.beskrivelse) {
                            "Det kreves beskrivelse når brukeren har uttalet seg. beskrivelse var null"
                        },
                    ),
                    kommentar = null,
                    behandler = behandler,
                )
            }
            UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL, UttalelseVurdering.UNNTAK_INGEN_UTTALELSE, UttalelseVurdering.NEI -> {
                tilbakekreving.lagreUttalelse(
                    uttalelseVurdering = uttalelseVurdering,
                    uttalelseInfo = null,
                    kommentar = requireNotNull(uttalelseDto.beskrivelse) {
                        "Det kreves kommentar/beskrivelse når brukeren ikke uttalte seg. beskrivelse var null"
                    },
                    behandler = behandler,
                )
            }
        }
    }

    fun nyUtsettUttalelsesfrist(
        tilbakekreving: Tilbakekreving,
        utsettFristDto: UpdateUttalelsesfristDto,
        behandler: Behandler,
    ): UttalelsesfristDto {
        return tilbakekreving.lagreFristUtsettelse(utsettFristDto.nyFrist!!, utsettFristDto.begrunnelse!!, behandler)
    }

    fun nyLagreForhåndsvarselUnntak(tilbakekreving: Tilbakekreving, unntakDto: ForhaandsvarselUnntakDto, behandler: Behandler) {
        tilbakekreving.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = when (unntakDto.begrunnelseForUnntak) {
                VarslingsUnntakDto.IKKE_PRAKTISK_MULIG -> BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG
                VarslingsUnntakDto.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                VarslingsUnntakDto.ÅPENBART_UNØDVENDIG -> BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG
                VarslingsUnntakDto.ALLEREDE_UTTALET_SEG -> BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG
            },
            beskrivelse = unntakDto.beskrivelse,
            behandler = behandler,
        )
    }

    fun hentForhåndsvarselinfo(tilbakekreving: Tilbakekreving): ForhåndsvarselDto {
        return tilbakekreving.hentForhåndsvarselFrontendDto()
    }

    fun utsettUttalelseFrist(
        behandler: Behandler,
        tilbakekreving: Tilbakekreving,
        fristUtsettelseDto: FristUtsettelseDto,
    ) {
        requireNotNull(tilbakekreving.brevHistorikk.sisteVarselbrev()) {
            "Kan ikke utsette frist når forhåndsvarsel ikke er sendt"
        }
        tilbakekreving.lagreFristUtsettelse(fristUtsettelseDto.nyFrist!!, fristUtsettelseDto.begrunnelse!!, behandler = behandler)
    }

    fun håndterForhåndsvarselUnntak(
        tilbakekreving: Tilbakekreving,
        forhåndsvarselUnntakDto: ForhåndsvarselUnntakDto,
        behandler: Behandler,
    ) {
        tilbakekreving.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = when (forhåndsvarselUnntakDto.begrunnelseForUnntak) {
                VarslingsUnntak.IKKE_PRAKTISK_MULIG -> BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG
                VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                VarslingsUnntak.ÅPENBART_UNØDVENDIG -> BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG
            },
            beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
            behandler = behandler,
        )
    }

    private fun opprettMetadata(varselbrevInfo: VarselbrevInfo): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevInfo.brukerinfo.ident,
            sakspartsnavn = varselbrevInfo.brukerinfo.navn,
            mottageradresse = Adresseinfo(varselbrevInfo.brukerinfo.ident, varselbrevInfo.brukerinfo.navn),
            behandlendeEnhetsNavn = requireNotNull(varselbrevInfo.forhåndsvarselinfo.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(varselbrevInfo.forhåndsvarselinfo.ansvarligSaksbehandler.ident),
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
            hjemlerForTilbakekreving = BrevFormatterer.lagForhåndsvarselHjemmelAvsnitt(varselbrevInfo.hjemlerForTilbakekreving, Språkkode.NB),
        )
    }

    fun journalførVarselbrev(
        varselbrevBehov: VarselbrevJournalføringBehov,
        logContext: SecureLog.Context,
    ): OpprettJournalpostResponse {
        val fristForUttalelse = varselbrevBehov.varselbrev.fristForUttalelse
        val brevdata = hentBrevdata(varselbrevBehov, fristForUttalelse, logContext)
        val dokument = Dokument(
            dokument = hentPdf(brevdata, logContext),
            filtype = Filtype.PDFA,
            filnavn = "brev.pdf",
            tittel = brevdata.tittel,
            dokumenttype = null,
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
        varselbrevBehov: VarselbrevJournalføringBehov,
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
            hjemlerForTilbakekreving = BrevFormatterer.lagForhåndsvarselHjemmelAvsnitt(varselbrevBehov.hjemlerForTilbakekreving, Språkkode.NB),
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

    private fun hentBrevMetadata(varselbrevBehov: VarselbrevJournalføringBehov): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevBehov.brukerinfo.ident,
            sakspartsnavn = varselbrevBehov.brukerinfo.navn,
            tittel = hentVarselbrevTittel(varselbrevBehov),
            mottageradresse = Adresseinfo(varselbrevBehov.brukerinfo.ident, varselbrevBehov.brukerinfo.navn),
            behandlendeEnhetsNavn = requireNotNull(varselbrevBehov.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(varselbrevBehov.varselbrev.ansvarligSaksbehandlerIdent),
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

    private fun hentVarselbrevTittel(varselbrevBehov: VarselbrevJournalføringBehov): String {
        return "$TITTEL_VARSEL_TILBAKEBETALING ${varselbrevBehov.ytelse.hentYtelsesnavn(Språkkode.NB)}"
    }

    fun nyHentForhåndsvarselinfo(tilbakekreving: Tilbakekreving): ForhaandsvarselResponseDto {
        return tilbakekreving.nyHentForhåndsvarselFrontendDto()
    }
}
