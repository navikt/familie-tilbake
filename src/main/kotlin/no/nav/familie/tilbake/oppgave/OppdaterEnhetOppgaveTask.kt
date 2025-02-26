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
    taskStepType = OppdaterEnhetOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer enhet på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterEnhetOppgaveTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<OppdaterEnhetOppgaveTask>()

    override fun doTask(task: Task) {
        val logContext = task.logContext()
        log.medContext(logContext) { info("OppdaterEnhetOppgaveTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString()) }
        val enhetId = task.metadata.getProperty("enhetId")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val saksbehandler = task.metadata.getProperty("saksbehandler")
        val behandlingId = UUID.fromString(task.payload)

        oppgaveService.oppdaterEnhetOgSaksbehandler(behandlingId, enhetId, beskrivelse, logContext, saksbehandler)
    }

    companion object {
        const val TYPE = "oppdaterEnhetOppgave"
    }
}
