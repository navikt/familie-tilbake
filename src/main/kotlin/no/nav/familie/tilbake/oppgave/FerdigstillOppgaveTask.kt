package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = LagOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Ferdigstiller oppgave for behandling",
                     triggerTidVedFeilISekunder = 60 * 5)
class FerdigstillOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("FerdigstillOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        oppgaveService.ferdigstillOppgave(behandlingId = UUID.fromString(task.payload), Oppgavetype.BehandleSak)
    }

    companion object {

        const val TYPE = "ferdigstillOppgave"
    }
}