package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.common.exceptionhandler.ManglerOppgaveFeil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
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
                log.error("Fant ingen oppgave å oppdatere på behandling $behandlingId.")
                throw e
            }

        val nyBeskrivelse =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) + ":" +
                beskrivelse + System.lineSeparator() + (oppgave?.beskrivelse ?: "")

        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)
        log.info("=====>>>>> TEST saksbehandler <<<====== {}", saksbehandler)
        log.info("=====>>>>> TEST oppgave.tildeltEnhetsnr <<<====== {}", oppgave.tildeltEnhetsnr)
        log.info("=====>>>>> TEST oppgave.tilordnetRessurs <<<====== {}", oppgave.tilordnetRessurs)


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
