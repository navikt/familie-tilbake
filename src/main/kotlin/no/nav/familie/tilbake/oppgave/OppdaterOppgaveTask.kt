package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.exceptionhandler.ManglerOppgaveFeil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterOppgaveTask(
    private val oppgaveService: OppgaveService,
    val environment: Environment,
    private val oppgavePrioritetService: OppgavePrioritetService,
    private val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<OppdaterOppgaveTask>()

    override fun doTask(task: Task) {
        val logContext = task.logContext()
        log.medContext(logContext) { info("OppdaterOppgaveTask prosesserer med id={}", task.id) }
        if (environment.activeProfiles.contains("e2e")) return

        val frist = task.metadata.getProperty("frist")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val saksbehandler =
            task.metadata.getProperty("saksbehandler")?.takeIf { saksbehandler ->
                saksbehandler.isNotBlank() && saksbehandler != Constants.BRUKER_ID_VEDTAKSLØSNINGEN
            }
        val behandlingId = UUID.fromString(task.payload)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR) {
            return
        }

        val oppgave =
            try {
                oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
            } catch (e: ManglerOppgaveFeil) {
                log.medContext(logContext) { error("Fant ingen oppgave å oppdatere på behandling {}.", behandlingId.toString()) }
                throw e
            }

        val nyBeskrivelse =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) + ":" +
                beskrivelse + System.lineSeparator() + (oppgave?.beskrivelse ?: "")

        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        var patchetOppgave =
            oppgave.copy(
                fristFerdigstillelse = frist,
                beskrivelse = nyBeskrivelse,
                prioritet = prioritet,
            )
        if (saksbehandler != null) {
            patchetOppgave = patchetOppgave.copy(tilordnetRessurs = saksbehandler)
        }
        oppgaveService.patchOppgave(patchetOppgave)
    }

    companion object {
        const val TYPE = "oppdaterOppgave"
    }
}
