package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.micrometer.AsyncTaskStepMedMetrics
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterPrioritetTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer prioritet på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterPrioritetTask(
    private val oppgaveService: OppgaveService,
    private val oppgavePrioritetService: OppgavePrioritetService,
) : AsyncTaskStepMedMetrics() {
    private val log = TracedLogger.getLogger<OppdaterPrioritetTask>()

    override fun doTaskWithMetrics(task: Task) {
        log.medContext(task.logContext()) { info("OppdaterPrioritetTask prosesserer med id={}", task.id) }
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        oppgaveService.patchOppgave(oppgave.copy(prioritet = prioritet))
    }

    companion object {
        const val TYPE = "oppdaterPrioritetForOppgave"
    }
}
