package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = LagOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Lager oppgave for nye behandlinger",
                     triggerTidVedFeilISekunder = 300L)
class LagOppgaveTask(private val oppgaveService: OppgaveService,
                     private val behandlingskontrollService: BehandlingskontrollService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("LagOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val oppgavetype = Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
        val behandlingId = UUID.fromString(task.payload)

        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        val fristeUker = behandlingsstegstilstand?.venteårsak?.defaultVenteTidIUker ?: 0
        val beskrivelse = behandlingsstegstilstand?.venteårsak?.beskrivelse
        oppgaveService.opprettOppgave(behandlingId = UUID.fromString(task.payload),
                                      oppgavetype = oppgavetype,
                                      beskrivelse = beskrivelse,
                                      fristForFerdigstillelse = LocalDate.now().plusWeeks(fristeUker))
    }

    companion object {

        const val TYPE = "lagOppgave"
    }
}
