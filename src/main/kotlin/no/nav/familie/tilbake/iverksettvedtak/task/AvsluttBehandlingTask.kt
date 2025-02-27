package no.nav.familie.tilbake.iverksettvedtak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AvsluttBehandlingTask.TYPE,
    beskrivelse = "Avslutter behandling",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AvsluttBehandlingTask(
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<AvsluttBehandlingTask>()

    @Transactional
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = task.logContext()
        log.medContext(logContext) {
            info("AvsluttBehandlingTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }

        behandlingService.avslutt(behandlingId, logContext)
    }

    companion object {
        const val TYPE = "avsluttBehandling"
    }
}
