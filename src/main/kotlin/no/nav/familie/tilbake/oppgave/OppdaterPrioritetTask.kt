package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
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
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<OppdaterPrioritetTask>()

    override fun doTask(task: Task) {
        log.medContext(task.logContext()) { info("OppdaterPrioritetTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString()) }
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        oppgaveService.patchOppgave(oppgave.copy(prioritet = prioritet))
    }

    companion object {
        const val TYPE = "oppdaterPrioritetForOppgave"
    }
}
