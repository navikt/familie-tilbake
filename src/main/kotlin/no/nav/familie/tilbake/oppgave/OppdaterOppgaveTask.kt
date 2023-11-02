package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.common.exceptionhandler.ManglerOppgaveFeil
import no.nav.familie.tilbake.config.Constants
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate
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
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        if (environment.activeProfiles.contains("e2e")) return

        val frist = task.metadata.getProperty("frist")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val saksbehandler = task.metadata.getProperty("saksbehandler").takeIf { saksbehandler ->
            saksbehandler.isNotBlank() && saksbehandler != Constants.BRUKER_ID_VEDTAKSLØSNINGEN
        }
        val behandlingId = UUID.fromString(task.payload)
        val enhet = task.metadata.getProperty("enhet")

        val oppgave = try {
            oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        } catch (e: ManglerOppgaveFeil) {
            log.warn("Fant ingen oppgave å oppdatere på behandling $behandlingId. Vil forsøke å opprette en ny isteden")
            null
        }

        val nyBeskrivelse = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")) + ":" +
            beskrivelse + System.lineSeparator() + (oppgave?.beskrivelse ?: "")

        if (oppgave == null) {
            val oppgavetype = oppgaveService.utledOppgavetypeForGjenoppretting(behandlingId)
            val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId)

            oppgaveService.opprettOppgave(
                behandlingId = behandlingId,
                beskrivelse = nyBeskrivelse,
                enhet = enhet,
                fristForFerdigstillelse = LocalDate.parse(frist),
                oppgavetype = oppgavetype,
                saksbehandler = saksbehandler,
                prioritet = prioritet,
            ).also {
                log.info("Ny oppgave (id=${it.oppgaveId}, type=$oppgavetype, frist=$frist) opprettet for behandling $behandlingId")
            }
            return
        }

        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandlingId, oppgave)

        var patchetOppgave = oppgave.copy(
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
