package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterAnsvarligSaksbehandlerTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer saksbehandler på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterAnsvarligSaksbehandlerTask(
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
    private val oppgavePrioritetService: OppgavePrioritetService,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterSaksbehandlerPåOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        if (oppgave.tilordnetRessurs != behandling.ansvarligSaksbehandler || oppgave.prioritet != prioritet) {
            try {
                oppgaveService.patchOppgave(oppgave.copy(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = prioritet))
            } catch (e: Exception) {
                oppgaveService.patchOppgave(oppgave.copy(prioritet = prioritet))
                SecureLog
                    .medContext(logContext) {
                        warn("Kunne ikke oppdatere tilordnetRessurs, ${behandling.ansvarligSaksbehandler}")
                    }
            }
        }
    }

    companion object {
        const val TYPE = "oppdaterSaksbehandlerOppgave"
    }
}
