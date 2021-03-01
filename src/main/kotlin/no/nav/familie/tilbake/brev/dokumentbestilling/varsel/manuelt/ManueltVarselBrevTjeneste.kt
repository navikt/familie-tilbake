package no.nav.familie.tilbake.brev.dokumentbestilling.varsel.manuelt

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.brev.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMottaker
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMottakerUtil
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.EksternDataForBrevTjeneste
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.pdf.BrevData
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.pdf.PdfBrevTjeneste
import no.nav.familie.tilbake.brev.dokumentbestilling.fritekstbrev.FritekstbrevData
import no.nav.familie.tilbake.brev.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.familie.tilbake.brev.dokumentbestilling.varsel.VarselbrevSamletInfo
import no.nav.familie.tilbake.brev.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import org.springframework.stereotype.Service
import java.time.Period
import java.util.UUID

@Service
class ManueltVarselBrevTjeneste(private val behandlingRepository: BehandlingRepository,
                                private val fagsakRepository: FagsakRepository,
                                private val eksternDataForBrevTjeneste: EksternDataForBrevTjeneste,
                                private val pdfBrevTjeneste: PdfBrevTjeneste,
                                private val faktaFeilutbetalingService: FaktaFeilutbetalingService) {

    val brukersSvarfrist: Period = Period.ofDays(14)


    fun sendManueltVarselBrev(behandlingId: UUID, fritekst: String, brevMottaker: BrevMottaker) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevSamletInfo = lagVarselBeløpForSending(fritekst, behandling, fagsak, brevMottaker, false)
        val data = lagManueltVarselBrev(varselbrevSamletInfo)
        val varsletFeilutbetaling = varselbrevSamletInfo.sumFeilutbetaling
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

    fun hentForhåndsvisningManueltVarselbrev(behandlingId: UUID, malType: Dokumentmalstype, fritekst: String): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val brevMottaker = if (behandling.harVerge) BrevMottaker.VERGE else BrevMottaker.BRUKER
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
        return pdfBrevTjeneste.genererForhåndsvisning(BrevData(mottaker = brevMottaker,
                                                               overskrift = data.overskrift,
                                                               brevtekst = data.brevtekst,
                                                               metadata = data.brevMetadata))
    }

    fun sendKorrigertVarselBrev(behandlingId: UUID, fritekst: String, brevMottaker: BrevMottaker) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevSamletInfo = lagVarselBeløpForSending(fritekst, behandling, fagsak, brevMottaker, true)
        val varsel: Varsel? = behandling.aktivtVarsel
        val data = lagKorrigertVarselBrev(varselbrevSamletInfo, varsel!!)
        val varsletFeilutbetaling = varselbrevSamletInfo.sumFeilutbetaling
        pdfBrevTjeneste.sendBrev(behandling,
                                 fagsak,
                                 Brevtype.KORRIGERT_VARSEL,
                                 BrevData(mottaker = brevMottaker,
                                          overskrift = data.overskrift,
                                          brevtekst = data.brevtekst,
                                          metadata = data.brevMetadata),
                                 varsletFeilutbetaling,
                                 fritekst)
    }

    private fun lagManueltVarselBrev(varselbrevSamletInfo: VarselbrevSamletInfo?): FritekstbrevData {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevOverskrift(varselbrevSamletInfo!!.brevMetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo)
        return FritekstbrevData(overskrift = overskrift,
                                brevtekst = brevtekst,
                                brevMetadata = varselbrevSamletInfo.brevMetadata)
    }

    private fun lagKorrigertVarselBrev(varselbrevSamletInfo: VarselbrevSamletInfo?, varselInfo: Varsel): FritekstbrevData {
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevOverskrift(varselbrevSamletInfo!!.brevMetadata)
        val brevtekst = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo, varselInfo)
        return FritekstbrevData(overskrift = overskrift,
                                brevtekst = brevtekst,
                                brevMetadata = varselbrevSamletInfo.brevMetadata)
    }

    private fun lagVarselBeløpForSending(fritekst: String,
                                         behandling: Behandling,
                                         fagsak: Fagsak,
                                         brevMottaker: BrevMottaker,
                                         erKorrigert: Boolean): VarselbrevSamletInfo {
        //Henter data fra pdl
        val personinfo = eksternDataForBrevTjeneste.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo =
                eksternDataForBrevTjeneste.hentAdresse(personinfo, brevMottaker, behandling.aktivVerge, fagsak.fagsystem)
        val vergeNavn: String = BrevMottakerUtil.getVergeNavn(behandling.aktivVerge, adresseinfo)


        //Henter feilutbetaling fakta

        //Henter feilutbetaling fakta
        val feilutbetalingFakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)

        return VarselbrevUtil.sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(behandling,
                                                                                       personinfo,
                                                                                       adresseinfo,
                                                                                       fagsak,
                                                                                       brukersSvarfrist,
                                                                                       fritekst,
                                                                                       feilutbetalingFakta,
                                                                                       behandling.harVerge,
                                                                                       vergeNavn,
                                                                                       erKorrigert)
    }

}
