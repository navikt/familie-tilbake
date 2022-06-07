package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VarselbrevService(
    private val fagsakRepository: FagsakRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val varselbrevUtil: VarselbrevUtil
) {

    fun sendVarselbrev(behandling: Behandling, brevmottager: Brevmottager) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevsdokument = lagVarselbrevForSending(behandling, fagsak, brevmottager)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val varsletFeilutbetaling = varselbrevsdokument.beløp
        val fritekst = varselbrevsdokument.varseltekstFraSaksbehandler
        val vedlegg = varselbrevUtil.lagVedlegg(
            varselbrevsdokument,
            behandling.aktivFagsystemsbehandling.eksternId,
            varselbrevsdokument.beløp
        )

        pdfBrevService.sendBrev(
            behandling,
            fagsak,
            Brevtype.VARSEL,
            Brevdata(
                mottager = brevmottager,
                metadata = varselbrevsdokument.brevmetadata,
                overskrift = overskrift,
                brevtekst = brevtekst,
                vedleggHtml = vedlegg
            ),
            varsletFeilutbetaling,
            fritekst
        )
    }

    private fun lagVarselbrevForSending(
        behandling: Behandling,
        fagsak: Fagsak,
        brevmottager: Brevmottager
    ): Varselbrevsdokument {
        val verge = behandling.aktivVerge

        // Henter data fra pdl
        val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, verge, fagsak.fagsystem)
        val vergenavn: String = BrevmottagerUtil.getVergenavn(verge, adresseinfo)

        return varselbrevUtil.sammenstillInfoFraFagsystemerForSending(
            fagsak,
            behandling,
            adresseinfo,
            personinfo,
            behandling.aktivtVarsel,
            vergenavn
        )
    }

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val varselbrevsdokument = lagVarselbrevForForhåndsvisning(forhåndsvisVarselbrevRequest)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val data = Fritekstbrevsdata(
            overskrift = overskrift,
            brevtekst = brevtekst,
            brevmetadata = varselbrevsdokument.brevmetadata
        )
        val brevmottager = if (varselbrevsdokument.brevmetadata.finnesVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        val vedlegg = varselbrevUtil.lagVedlegg(
            varselbrevsdokument,
            forhåndsvisVarselbrevRequest.fagsystemsbehandlingId,
            varselbrevsdokument.beløp
        )
        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                metadata = data.brevmetadata,
                overskrift = data.overskrift,
                brevtekst = data.brevtekst,
                vedleggHtml = vedlegg
            )
        )
    }

    private fun lagVarselbrevForForhåndsvisning(request: ForhåndsvisVarselbrevRequest): Varselbrevsdokument {

        val brevmottager = if (request.verge != null) Brevmottager.VERGE else Brevmottager.BRUKER
        val personinfo = eksterneDataForBrevService.hentPerson(request.ident, request.fagsystem)
        val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(
            personinfo,
            brevmottager,
            request.verge,
            request.fagsystem
        )

        return varselbrevUtil.sammenstillInfoForForhåndvisningVarselbrev(
            adresseinfo,
            request,
            personinfo
        )
    }
}
