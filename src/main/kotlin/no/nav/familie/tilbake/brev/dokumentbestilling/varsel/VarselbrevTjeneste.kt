package no.nav.familie.tilbake.brev.dokumentbestilling.varsel

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMottaker
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMottakerUtil
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.EksternDataForBrevTjeneste
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.pdf.BrevData
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.pdf.PdfBrevTjeneste
import no.nav.familie.tilbake.brev.dokumentbestilling.fritekstbrev.FritekstbrevData
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.integration.pdl.PdlClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class VarselbrevTjeneste(private val behandlingRepository: BehandlingRepository,
                         private val fagsakRepository: FagsakRepository,
                         private val pdlClient: PdlClient,
                         private val eksternDataForBrevTjeneste: EksternDataForBrevTjeneste,
                         private val pdfBrevTjeneste: PdfBrevTjeneste) {

    fun sendVarselbrev(behandlingId: UUID, brevMottaker: BrevMottaker) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevSamletInfo = lagVarselbrevForSending(behandling, fagsak, brevMottaker)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevOverskrift(varselbrevSamletInfo.brevMetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo)
        val data = FritekstbrevData(overskrift = overskrift,
                                    brevtekst = brevtekst,
                                    brevMetadata = varselbrevSamletInfo.brevMetadata)

        val varsletFeilutbetaling = varselbrevSamletInfo.sumFeilutbetaling
        val fritekst = varselbrevSamletInfo.fritekstFraSaksbehandler

        pdfBrevTjeneste.sendBrev(behandling,
                                 fagsak,
                                 Brevtype.VARSEL,
                                 BrevData(mottaker = brevMottaker,
                                          metadata = data.brevMetadata,
                                          overskrift = data.overskrift,
                                          brevtekst = data.brevtekst),
                                 varsletFeilutbetaling,
                                 fritekst)
    }

    private fun lagVarselbrevForSending(behandling: Behandling,
                                        fagsak: Fagsak,
                                        brevMottaker: BrevMottaker): VarselbrevSamletInfo {
        val verge = behandling.aktivVerge

        //Henter data fra pdl
        val personinfo = pdlClient.hentPersoninfo(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo = eksternDataForBrevTjeneste.hentAdresse(personinfo, brevMottaker, verge, fagsak.fagsystem)
        val vergeNavn: String = BrevMottakerUtil.getVergeNavn(verge, adresseinfo)

        return VarselbrevUtil.sammenstillInfoFraFagsystemerForSending(fagsak,
                                                                      behandling,
                                                                      adresseinfo,
                                                                      personinfo,
                                                                      Constants.brukersSvarfrist,
                                                                      behandling.aktivtVarsel,
                                                                      behandling.harVerge,
                                                                      vergeNavn)
    }

}
