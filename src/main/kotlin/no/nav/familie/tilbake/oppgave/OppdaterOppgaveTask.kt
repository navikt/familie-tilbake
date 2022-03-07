package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID


@Service
@TaskStepBeskrivelse(taskStepType = OppdaterOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Oppdaterer oppgave",
                     triggerTidVedFeilISekunder = 300L)
class OppdaterOppgaveTask(private val oppgaveService: OppgaveService,
                          val environment: Environment) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        if (environment.activeProfiles.contains("e2e")) return

        val frist = task.metadata.getProperty("frist")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)

        val nyBeskrivelse = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm")) +":" +
                            beskrivelse + "\n" + oppgave.beskrivelse
        oppgaveService.patchOppgave(oppgave.copy(fristFerdigstillelse = frist,
                                                 beskrivelse = nyBeskrivelse))
    }

    companion object {

        const val TYPE = "oppdaterOppgave"
    }
}
