package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.api.dto.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
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
        val varselbrevsdokument = lagVarselbrevForSending(behandling, fagsak, brevmottager)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument)
        val varsletFeilutbetaling = varselbrevsdokument.beløp
        val fritekst = varselbrevsdokument.varseltekstFraSaksbehandler

        pdfBrevService.sendBrev(behandling,
                                fagsak,
                                Brevtype.VARSEL,
                                Brevdata(mottager = brevmottager,
                                         metadata = varselbrevsdokument.brevmetadata,
                                         overskrift = overskrift,
                                         brevtekst = brevtekst),
                                varsletFeilutbetaling,
                                fritekst)
    }

    private fun lagVarselbrevForSending(behandling: Behandling,
                                        fagsak: Fagsak,
                                        brevmottager: Brevmottager): Varselbrevsdokument {
        val verge = behandling.aktivVerge

        //Henter data fra pdl
        val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo = eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, verge, fagsak.fagsystem)
        val vergenavn: String = BrevmottagerUtil.getVergenavn(verge, adresseinfo)

        return VarselbrevUtil.sammenstillInfoFraFagsystemerForSending(fagsak,
                                                                      behandling,
                                                                      adresseinfo,
                                                                      personinfo,
                                                                      behandling.aktivtVarsel,
                                                                      vergenavn)
    }

    fun hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val varselbrevsdokument = lagVarselbrevForForhåndsvisning(forhåndsvisVarselbrevRequest)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument)
        val data = Fritekstbrevsdata(overskrift = overskrift,
                                     brevtekst = brevtekst,
                                     brevmetadata = varselbrevsdokument.brevmetadata)
        val brevmottager = if (varselbrevsdokument.brevmetadata.finnesVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        return pdfBrevService.genererForhåndsvisning(Brevdata(mottager = brevmottager,
                                                              metadata = data.brevmetadata,
                                                              overskrift = data.overskrift,
                                                              brevtekst = data.brevtekst))
    }

    private fun lagVarselbrevForForhåndsvisning(request: ForhåndsvisVarselbrevRequest): Varselbrevsdokument {

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
