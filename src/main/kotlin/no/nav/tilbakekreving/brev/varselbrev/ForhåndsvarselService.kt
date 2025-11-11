package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
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
