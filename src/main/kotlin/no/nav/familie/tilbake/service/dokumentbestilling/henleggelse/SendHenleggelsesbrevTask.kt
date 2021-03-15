package no.nav.familie.tilbake.service.dokumentbestilling.henleggelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID


@Component
@TaskStepBeskrivelse(taskStepType = SendHenleggelsesbrevTask.TYPE,
                     maxAntallFeil = 50,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Send henleggelsesbrev.")
class SendHenleggelsesbrevTask(private val henleggelsesbrevService: HenleggelsesbrevService,
                               private val behandlingRepository: BehandlingRepository) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskdata: SendBrevTaskdata = objectMapper.readValue(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(taskdata.behandlingId)

        val brevmottager = if (behandling.harVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        henleggelsesbrevService.sendHenleggelsebrev(behandling.id, taskdata.fritekst, brevmottager)
    }

    companion object {

        fun opprettTask(behandlingId: UUID,
                        fritekst: String?): Task =
                Task(type = TYPE,
                     payload = objectMapper.writeValueAsString(SendBrevTaskdata(behandlingId, fritekst)),
                     triggerTid = LocalDateTime.now().plusSeconds(15))

        const val TYPE = "distribuerVedtaksbrev"
    }


}

data class SendBrevTaskdata(val behandlingId: UUID,
                            val fritekst: String?)