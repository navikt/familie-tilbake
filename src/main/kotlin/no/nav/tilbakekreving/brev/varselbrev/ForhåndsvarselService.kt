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
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.UtsettFristInfo
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
            HarBrukerUttaltSeg.JA -> {
                val uttalelsedetaljer = requireNotNull(brukeruttalelse.uttalelsesdetaljer) {
                    "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null"
                }.also {
                    require(it.isNotEmpty()) {
                        "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var tøm"
                    }
                }
                behandling.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = uttalelsedetaljer.map { UttalelseInfo(UUID.randomUUID(), it.uttalelsesdato, it.hvorBrukerenUttalteSeg, it.uttalelseBeskrivelse) },
                    kommentar = null,
                    utsettFrist = listOf(),
                )
            }
            HarBrukerUttaltSeg.NEI -> {
                val kommentar = requireNotNull(brukeruttalelse.kommentar) {
                    "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var null"
                }.also {
                    require(it.isNotBlank()) { "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var tøm" }
                }

                behandling.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = listOf(),
                    kommentar = kommentar,
                    utsettFrist = listOf(),
                )
            }
            HarBrukerUttaltSeg.UTTSETT_FRIST -> {
                requireNotNull(tilbakekreving.brevHistorikk.sisteVarselbrev()) {
                    "Kan ikke utsette frist når forhåndsvarsel ikke er sendt"
                }
                val utsattFrist = requireNotNull(brukeruttalelse.utsettFrist) { "Det kreves en ny dato når fristen er utsatt. utsettFrist var null" }
                    .also {
                        require(it.isNotEmpty()) { "Det kreves en ny dato når fristen er utsatt. utsettFrist var tøm" }
                    }

                behandling.lagreUttalelse(
                    uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                    uttalelseInfo = listOf(),
                    kommentar = null,
                    utsettFrist = utsattFrist.map { UtsettFristInfo(UUID.randomUUID(), it.nyFrist, it.begrunnelse) },
                )
            }
            HarBrukerUttaltSeg.ALLEREDE_UTTALET_SEG -> {
                throw Feil(
                    message = "Feil i hådntering av uttalelse for behandlingId ${behandling.id}. Hvis ingen forhåndsvarsel er sendt må hådnteres denne i forhåndsvarselUnntak.",
                    frontendFeilmelding = "Feil i hådntering av uttalelse for behandlingId ${behandling.id}. Hvis ingen forhåndsvarsel er sendt må hådnteres denne i forhåndsvarselUnntak.",
                    logContext = SecureLog.Context.fra(tilbakekreving),
                )
            }
        }
    }

    fun hentForhåndsvarselinfo(tilbakekreving: Tilbakekreving): ForhåndsvarselDto {
        return tilbakekreving.hentForhåndsvarselFrontendDto()
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
                    uttalelseInfo = listOf(),
                )
            }
            VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> {
                behandling.lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
                    beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
                    uttalelseInfo = listOf(),
                )
            }
            VarslingsUnntak.ÅPENBART_UNØDVENDIG -> {
                behandling.lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                    beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
                    uttalelseInfo = listOf(),
                )
            }
            VarslingsUnntak.ALLEREDE_UTTALET_SEG -> {
                val uttalelsedetaljer = requireNotNull(forhåndsvarselUnntakDto.uttalelsesdetaljer) {
                    "Det kreves uttalelsedetaljer når brukeren har allerede uttalet seg. uttalelsedetaljer var null"
                }.also {
                    require(it.isNotEmpty()) {
                        "Det kreves uttalelsedetaljer når brukeren har allerede uttalet seg. uttalelsedetaljer var tøm"
                    }
                }
                behandling.lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
                    beskrivelse = forhåndsvarselUnntakDto.beskrivelse,
                    uttalelseInfo = uttalelsedetaljer.map {
                        UttalelseInfo(
                            id = UUID.randomUUID(),
                            uttalelsesdato = it.uttalelsesdato,
                            hvorBrukerenUttalteSeg = it.hvorBrukerenUttalteSeg,
                            uttalelseBeskrivelse = it.uttalelseBeskrivelse,
                        )
                    },
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
