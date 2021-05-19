package no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = SendManueltVarselbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender manuelt varselbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class SendManueltVarselbrevTask(val behandlingRepository: BehandlingRepository,
                                val manueltVarselBrevTjeneste: ManueltVarselbrevService,
                                val behandlingskontrollTjeneste: BehandlingskontrollService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val maltype = Dokumentmalstype.valueOf(task.metadata.getProperty("maltype"))
        val fritekst = task.metadata.getProperty("fritekst")
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        // sjekk om behandlingen har verge

        if (Dokumentmalstype.VARSEL == maltype) {
            if (behandling.harVerge) {
                manueltVarselBrevTjeneste.sendManueltVarselBrev(behandling, fritekst, Brevmottager.VERGE)
            }
            manueltVarselBrevTjeneste.sendManueltVarselBrev(behandling, fritekst, Brevmottager.BRUKER)

        } else if (Dokumentmalstype.KORRIGERT_VARSEL == maltype) {
            if (behandling.harVerge) {
                manueltVarselBrevTjeneste.sendKorrigertVarselBrev(behandling, fritekst, Brevmottager.VERGE)
            }
            manueltVarselBrevTjeneste.sendKorrigertVarselBrev(behandling, fritekst, Brevmottager.BRUKER)
        }
        val fristTid = LocalDate.now().plus(Constants.brukersSvarfrist).plusDays(1)
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling.id,
                                                         Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                         fristTid)
    }

    companion object {

        const val TYPE = "brev.sendManueltVarsel"
    }
}