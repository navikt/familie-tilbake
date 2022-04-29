package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = OppdaterTilordnetRessursOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Oppdaterer tilordnet ressurs på oppgave",
                     triggerTidVedFeilISekunder = 300L)
class OppdaterTilordnetRessursOppgaveTask(private val oppgaveService: OppgaveService,
                                          private val oppgaveTaskService: OppgaveTaskService,
                                          private val behandlingRepository: BehandlingRepository) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterSaksbehandlerPåOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)

        oppgaveService.patchOppgave(oppgave.copy(tilordnetRessurs = behandling.ansvarligSaksbehandler))
    }

    @Transactional
    override fun onCompletion(task: Task) {
        val opprettFerdigstillOppgaveTask = task.metadata.getProperty("opprettFerdigstillOppgaveTask")
        if (opprettFerdigstillOppgaveTask.toBoolean()) {
            log.info("Oppretter task for å ferdigstille oppgave")
            val behandlingId = UUID.fromString(task.payload)
            behandlingRepository.findByIdOrThrow(behandlingId)

            val oppgavetype = if (task.metadata.containsKey("ferdigstillOppgavetype")) {
                task.metadata.getProperty("ferdigstillOppgavetype")
            } else {
                null
            }
            oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId = behandlingId, oppgavetype = oppgavetype)
        }
    }

    companion object {

        // TODO: Rename denne til oppdaterTilordnetRessursOppgave? Brukes både for saksbehandler- og beslutter-oppgåver
        const val TYPE = "oppdaterSaksbehandlerOppgave"
    }
}
