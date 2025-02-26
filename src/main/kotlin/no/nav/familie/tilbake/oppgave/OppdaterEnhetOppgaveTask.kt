package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.kontrakter.Tema
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterEnhetOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Oppdaterer enhet på oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class OppdaterEnhetOppgaveTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<OppdaterEnhetOppgaveTask>()

    override fun doTask(task: Task) {
        log.medContext(task.logContext()) { info("OppdaterEnhetOppgaveTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString()) }
        val enhetId = task.metadata.getProperty("enhetId")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val saksbehandler = task.metadata.getProperty("saksbehandler")
        val behandlingId = UUID.fromString(task.payload)

        val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
        val nyBeskrivelse =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy hh:mm")) + ":" +
                beskrivelse + System.lineSeparator() + oppgave.beskrivelse
        var patchetOppgave = oppgave.copy(beskrivelse = nyBeskrivelse)
        if (!saksbehandler.isNullOrEmpty() && saksbehandler != Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            patchetOppgave = patchetOppgave.copy(tilordnetRessurs = saksbehandler)
        }

        if (oppgave.tema == Tema.ENF) {
            oppgaveService.tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, false) // ENF bruker generelle mapper
        } else {
            oppgaveService.tilordneOppgaveNyEnhet(oppgave.id!!, enhetId, true) // KON og BAR bruker mapper som hører til enhetene
        }

        oppgaveService.patchOppgave(patchetOppgave)
    }

    companion object {
        const val TYPE = "oppdaterEnhetOppgave"
    }
}
