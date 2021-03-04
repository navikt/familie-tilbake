package no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevSamletInfo
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevUtil
import org.springframework.stereotype.Service
import java.time.Period
import java.util.UUID

@Service
class ManueltVarselbrevService(private val behandlingRepository: BehandlingRepository,
                               private val fagsakRepository: FagsakRepository,
                               private val eksterneDataForBrevService: EksterneDataForBrevService,
                               private val pdfBrevService: PdfBrevService,
                               private val faktaFeilutbetalingService: FaktaFeilutbetalingService) {

    fun sendManueltVarselBrev(behandlingId: UUID, fritekst: String, brevmottager: Brevmottager) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevSamletInfo = lagVarselBeløpForSending(fritekst, behandling, fagsak, brevmottager, false)
        val data = lagManueltVarselBrev(varselbrevSamletInfo)
        val varsletFeilutbetaling = varselbrevSamletInfo.sumFeilutbetaling
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

    fun hentForhåndsvisningManueltVarselbrev(behandlingId: UUID, malType: Dokumentmalstype, fritekst: String): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val brevMottaker = if (behandling.harVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        val data = when (malType) {
            Dokumentmalstype.VARSEL -> {
                val varselbrevSamletInfo = lagVarselBeløpForSending(fritekst, behandling, fagsak, brevMottaker, false)
                lagManueltVarselBrev(varselbrevSamletInfo)
            }
            Dokumentmalstype.KORRIGERT_VARSEL -> {
                val varselbrevSamletInfo = lagVarselBeløpForSending(fritekst, behandling, fagsak, brevMottaker, true)
                val varsel: Varsel = behandling.aktivtVarsel!!
                lagKorrigertVarselBrev(varselbrevSamletInfo, varsel)
            }
            else -> {
                throw IllegalArgumentException("Ikke-støttet DokumentMalType: $malType")
            }
        }
        return pdfBrevService.genererForhåndsvisning(Brevdata(mottager = brevMottaker,
                                                              overskrift = data.overskrift,
                                                              brevtekst = data.brevtekst,
                                                              metadata = data.brevmetadata))
    }

    fun sendKorrigertVarselBrev(behandlingId: UUID, fritekst: String, brevmottager: Brevmottager) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevSamletInfo = lagVarselBeløpForSending(fritekst, behandling, fagsak, brevmottager, true)
        val varsel: Varsel? = behandling.aktivtVarsel
        val data = lagKorrigertVarselBrev(varselbrevSamletInfo, varsel!!)
        val varsletFeilutbetaling = varselbrevSamletInfo.sumFeilutbetaling
        pdfBrevService.sendBrev(behandling,
                                fagsak,
                                Brevtype.KORRIGERT_VARSEL,
                                Brevdata(mottager = brevmottager,
                                         overskrift = data.overskrift,
                                         brevtekst = data.brevtekst,
                                         metadata = data.brevmetadata),
                                varsletFeilutbetaling,
                                fritekst)
    }

    private fun lagManueltVarselBrev(varselbrevSamletInfo: VarselbrevSamletInfo?): Fritekstbrevsdata {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevSamletInfo!!.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        return Fritekstbrevsdata(overskrift = overskrift,
                                 brevtekst = brevtekst,
                                 brevmetadata = varselbrevSamletInfo.brevmetadata)
    }

    private fun lagKorrigertVarselBrev(varselbrevSamletInfo: VarselbrevSamletInfo?, varselInfo: Varsel): Fritekstbrevsdata {
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevsoverskrift(varselbrevSamletInfo!!.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo, varselInfo)
        return Fritekstbrevsdata(overskrift = overskrift,
                                 brevtekst = brevtekst,
                                 brevmetadata = varselbrevSamletInfo.brevmetadata)
    }

    private fun lagVarselBeløpForSending(fritekst: String,
                                         behandling: Behandling,
                                         fagsak: Fagsak,
                                         brevmottager: Brevmottager,
                                         erKorrigert: Boolean): VarselbrevSamletInfo {
        //Henter data fra pdl
        val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo =
                eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem)
        val vergeNavn: String = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)


        //Henter feilutbetaling fakta

        //Henter feilutbetaling fakta
        val feilutbetalingFakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)

        return VarselbrevUtil.sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(behandling,
                                                                                       personinfo,
                                                                                       adresseinfo,
                                                                                       fagsak,
                                                                                       fritekst,
                                                                                       feilutbetalingFakta,
                                                                                       behandling.harVerge,
                                                                                       vergeNavn,
                                                                                       erKorrigert)
    }

}
