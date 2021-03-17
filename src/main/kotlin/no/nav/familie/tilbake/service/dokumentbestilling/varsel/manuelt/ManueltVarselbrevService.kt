package no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service
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
        val varselbrevsdokument = lagVarselbrevForSending(fritekst,
                                                          behandling,
                                                          fagsak,
                                                          brevmottager,
                                                          false,
                                                          behandling.aktivtVarsel)
        val data = lagManueltVarselBrev(varselbrevsdokument)
        val varsletFeilutbetaling = varselbrevsdokument.beløp
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
        val brevmottager = if (behandling.harVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        val data = when (malType) {
            Dokumentmalstype.VARSEL -> {
                val varselbrevsdokument = lagVarselbrevForSending(fritekst,
                                                                  behandling,
                                                                  fagsak,
                                                                  brevmottager,
                                                                  false)
                lagManueltVarselBrev(varselbrevsdokument)
            }
            Dokumentmalstype.KORRIGERT_VARSEL -> {
                val varselbrevsdokument =
                        lagVarselbrevForSending(fritekst, behandling, fagsak, brevmottager, true, behandling.aktivtVarsel)
                lagKorrigertVarselbrev(varselbrevsdokument)
            }
            else -> {
                throw IllegalArgumentException("Ikke-støttet DokumentMalType: $malType")
            }
        }
        return pdfBrevService.genererForhåndsvisning(Brevdata(mottager = brevmottager,
                                                              overskrift = data.overskrift,
                                                              brevtekst = data.brevtekst,
                                                              metadata = data.brevmetadata))
    }

    fun sendKorrigertVarselBrev(behandlingId: UUID, fritekst: String, brevmottager: Brevmottager) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val varselbrevsdokument = lagVarselbrevForSending(fritekst,
                                                          behandling,
                                                          fagsak,
                                                          brevmottager,
                                                          true,
                                                          behandling.aktivtVarsel)
        val data = lagKorrigertVarselbrev(varselbrevsdokument)
        val varsletFeilutbetaling = varselbrevsdokument.beløp
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

    private fun lagManueltVarselBrev(varselbrevsdokument: Varselbrevsdokument): Fritekstbrevsdata {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument)
        return Fritekstbrevsdata(overskrift = overskrift,
                                 brevtekst = brevtekst,
                                 brevmetadata = varselbrevsdokument.brevmetadata)
    }

    private fun lagKorrigertVarselbrev(varselbrevsdokument: Varselbrevsdokument): Fritekstbrevsdata {
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevsoverskrift(varselbrevsdokument.brevmetadata)
        val brevtekst = TekstformatererVarselbrev.lagKorrigertFritekst(varselbrevsdokument)
        return Fritekstbrevsdata(overskrift = overskrift,
                                 brevtekst = brevtekst,
                                 brevmetadata = varselbrevsdokument.brevmetadata)
    }

    private fun lagVarselbrevForSending(fritekst: String,
                                        behandling: Behandling,
                                        fagsak: Fagsak,
                                        brevmottager: Brevmottager,
                                        erKorrigert: Boolean,
                                        aktivtVarsel: Varsel? = null): Varselbrevsdokument {
        //Henter data fra pdl
        val personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo =
                eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem)
        val vergenavn: String = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)


        //Henter feilutbetaling fakta
        val feilutbetalingsfakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandling.id)

        val metadata = VarselbrevUtil.sammenstillInfoForBrevmetadata(behandling,
                                                                     personinfo,
                                                                     adresseinfo,
                                                                     fagsak,
                                                                     vergenavn,
                                                                     erKorrigert)

        return VarselbrevUtil.sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(metadata,
                                                                                       fritekst,
                                                                                       feilutbetalingsfakta,
                                                                                       aktivtVarsel)
    }

}
