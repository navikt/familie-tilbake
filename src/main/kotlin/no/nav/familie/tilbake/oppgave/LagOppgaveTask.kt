package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = LagOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Lager oppgave for nye behandlinger",
                     triggerTidVedFeilISekunder = 60 * 5)
class LagOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("LagOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val oppgavetype = Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
        oppgaveService.opprettOppgave(behandlingId = UUID.fromString(task.payload),
                                      oppgavetype = oppgavetype,
                                      fristForFerdigstillelse = LocalDate.now().plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker))
    }

    companion object {

        const val TYPE = "lagOppgave"
    }
}
