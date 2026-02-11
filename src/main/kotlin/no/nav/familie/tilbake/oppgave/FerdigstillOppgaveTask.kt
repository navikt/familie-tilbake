package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Ferdigstiller oppgave for behandling",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class FerdigstillOppgaveTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<FerdigstillOppgaveTask>()

    override fun doTask(task: Task) {
        log.medContext(task.logContext()) { info("FerdigstillOppgaveTask prosesserer med id={}", task.id) }
        val oppgavetype =
            if (task.metadata.containsKey("oppgavetype")) {
                Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
            } else {
                null
            }
        oppgaveService.ferdigstillOppgave(
            behandlingId = UUID.fromString(task.payload),
            oppgavetype = oppgavetype,
        )
    }

    companion object {
        const val TYPE = "ferdigstillOppgave"
    }
}
