package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = SendVarselbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender varselbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class SendVarselbrevTask(private val varselbrevService: VarselbrevService,
                         private val behandlingRepository: BehandlingRepository) : AsyncTaskStep {


    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.harVerge) {
            varselbrevService.sendVarselbrev(behandling, Brevmottager.VERGE)
        }
        varselbrevService.sendVarselbrev(behandling, Brevmottager.BRUKER)
    }

    companion object {

        const val TYPE = "brev.sendVarsel"
    }

}