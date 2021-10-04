package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = OppdaterEnhetOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Oppdaterer enhet på oppgave",
                     triggerTidVedFeilISekunder = 300L)
class OppdaterEnhetOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterEnhetOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val enhetId = task.metadata.getProperty("enhetId")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val nyBeskrivelse = beskrivelse + "/n" + oppgave.beskrivelse
        oppgaveService.patchOppgave(oppgave.copy(tildeltEnhetsnr = enhetId, beskrivelse = nyBeskrivelse))
    }

    companion object {

        const val TYPE = "oppdaterEnhetOppgave"
    }
}
