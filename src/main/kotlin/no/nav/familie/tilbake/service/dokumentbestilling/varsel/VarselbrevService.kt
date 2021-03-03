package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.api.dto.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class VarselbrevService(private val behandlingRepository: BehandlingRepository,
                        private val fagsakRepository: FagsakRepository,
                        private val eksterneDataForBrevService: EksterneDataForBrevService,
                        private val pdfBrevService: PdfBrevService) {

    fun sendVarselbrev(behandlingId: UUID, brevmottager: Brevmottager) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevSamletInfo = lagVarselbrevForSending(behandling, fagsak, brevmottager)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevSamletInfo.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        val data = Fritekstbrevsdata(overskrift = overskrift,
                                     brevtekst = brevtekst,
                                     brevmetadata = varselbrevSamletInfo.brevmetadata)

        val varsletFeilutbetaling = varselbrevSamletInfo.sumFeilutbetaling
        val fritekst = varselbrevSamletInfo.fritekstFraSaksbehandler

        pdfBrevService.sendBrev(behandling,
                                fagsak,
                                Brevtype.VARSEL,
                                Brevdata(mottager = brevmottager,
                                         metadata = data.brevmetadata,
                                         overskrift = data.overskrift,
                                         brevtekst = data.brevtekst),
                                varsletFeilutbetaling,
                                fritekst)
    }

    private fun lagVarselbrevForSending(behandling: Behandling,
                                        fagsak: Fagsak,
                                        brevmottager: Brevmottager): VarselbrevSamletInfo {
        val verge = behandling.aktivVerge

        //Henter data fra pdl
        val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, verge, fagsak.fagsystem)
        val vergeNavn: String = BrevmottagerUtil.getVergenavn(verge, adresseinfo)

        return VarselbrevUtil.sammenstillInfoFraFagsystemerForSending(fagsak,
                                                                      behandling,
                                                                      adresseinfo,
                                                                      personinfo,
                                                                      behandling.aktivtVarsel,
                                                                      behandling.harVerge,
                                                                      vergeNavn)
    }

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val varselbrevSamletInfo: VarselbrevSamletInfo =
                lagVarselbrevForForhåndsvisning(forhåndsvisVarselbrevRequest)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevSamletInfo.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        val brevMottaker = if (varselbrevSamletInfo.brevmetadata.finnesVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        val data = Fritekstbrevsdata(overskrift = overskrift,
                                     brevtekst = brevtekst,
                                     brevmetadata = varselbrevSamletInfo.brevmetadata)
        return pdfBrevService.genererForhåndsvisning(Brevdata(mottager = brevMottaker,
                                                              metadata = data.brevmetadata,
                                                              overskrift = data.overskrift,
                                                              brevtekst = data.brevtekst)
        )
    }

    private fun lagVarselbrevForForhåndsvisning(request: ForhåndsvisVarselbrevRequest): VarselbrevSamletInfo {

        val brevmottager = if (request.verge != null) Brevmottager.VERGE else Brevmottager.BRUKER
        val personinfo = eksterneDataForBrevService.hentPerson(request.ident, request.fagsystem)
        val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(personinfo,
                                                                              brevmottager,
                                                                              request.verge,
                                                                              request.fagsystem)

        return VarselbrevUtil.sammenstillInfoForForhåndvisningVarselbrev(adresseinfo,
                                                                         request,
                                                                         personinfo)
    }

}
