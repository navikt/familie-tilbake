package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID


@Service
@TaskStepBeskrivelse(taskStepType = OppdaterOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Oppdaterer oppgave",
                     triggerTidVedFeilISekunder = 300L)
class OppdaterOppgaveTask(private val oppgaveService: OppgaveService,
                          private val integrasjonerClient: IntegrasjonerClient,
                          private val fagsakRepository: FagsakRepository,
                          private val behandlingRepository: BehandlingRepository) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val oppgavetype = Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
        val frist = task.metadata.getProperty("frist")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val behandlingId = UUID.fromString(task.payload)


        val oppgave = oppgaveService.finnOppgaveForBehandling(behandlingId, oppgavetype)

        val nyBeskrivelse = beskrivelse + "\n" + oppgave.beskrivelse
        oppgaveService.patchOppgave(oppgave.copy(fristFerdigstillelse = frist,
                                                 beskrivelse = nyBeskrivelse))
    }

    companion object {

        const val TYPE = "oppdaterOppgave"
    }
}
