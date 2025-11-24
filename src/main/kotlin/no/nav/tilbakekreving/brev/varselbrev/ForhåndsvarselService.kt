package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.log.SecureLog
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
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ForhåndsvarselService(
    private val pdfBrevService: PdfBrevService,
    private val varselbrevUtil: VarselbrevUtil,
    private val dokarkivClient: DokarkivClient,
    private val dokdistClient: DokdistClient,
) {
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

        val varselbrevBehov = tilbakekreving.opprettVarselbrevBehov(bestillBrevDto.fritekst)
        val journalpost = dokarkivClient.journalførVarselbrev(varselbrevBehov, logContext)
        if (journalpost.journalpostId == null) {
            throw Feil(
                message = "journalførin av varselbrev til behandlingId ${varselbrevBehov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                frontendFeilmelding = "journalførin av varselbrev til behandlingId ${varselbrevBehov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                logContext = SecureLog.Context.fra(tilbakekreving),
            )
        }
        dokdistClient.brevTilUtsending(varselbrevBehov, journalpost.journalpostId, logContext)
        tilbakekreving.oppdaterSendtVarselbrev(journalpostId = journalpost.journalpostId, varselbrevId = varselbrevBehov.varselbrev.id)
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
        when (forhåndsvarselUnntakDto.begrunnelseForUnntak) {
            VarslingsUnntak.IKKE_PRAKTISK_MULIG -> {
                behandling.lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                    beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
                )
            }
            VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> {
                behandling.lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
                    beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
                )
            }
            VarslingsUnntak.ÅPENBART_UNØDVENDIG -> {
                behandling.lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                    beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
                )
            }
        }
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
}
