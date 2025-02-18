package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskGjenopptaBehandlingTask.TYPE,
    beskrivelse = "gjenopptar behandling automatisk",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AutomatiskGjenopptaBehandlingTask(
    private val automatiskGjenopptaBehandlingService: AutomatiskGjenopptaBehandlingService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<AutomatiskGjenopptaBehandlingTask>()

    override fun doTask(task: Task) {
        log.medContext(task.logContext()) { info("AutomatiskGjenopptaBehandlingTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString()) }
        val behandlingId = UUID.fromString(task.payload)
        automatiskGjenopptaBehandlingService.gjenopptaBehandling(behandlingId, task.id, task.metadata)
    }

    companion object {
        const val TYPE = "gjenoppta.behandling.automatisk"
    }
}
