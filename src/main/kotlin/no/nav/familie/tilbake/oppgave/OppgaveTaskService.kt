package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(private val taskRepository: TaskRepository) {

    @Transactional
    fun opprettOppgaveTask(behandlingId: UUID, oppgavetype: Oppgavetype, saksbehandler: String? = null) {
        val properties = Properties()
        properties.setProperty("oppgavetype", oppgavetype.name)
        saksbehandler?.let { properties.setProperty("saksbehandler", it) }
        taskRepository.save(Task(type = LagOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun ferdigstilleOppgaveTask(behandlingId: UUID, oppgavetype: String? = null) {
        val properties = Properties()
        if (!oppgavetype.isNullOrEmpty()) {
            properties.setProperty("oppgavetype", oppgavetype)
        }
        taskRepository.save(Task(type = FerdigstillOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterOppgaveTask(behandlingId: UUID, beskrivelse: String, frist: LocalDate) {
        val properties = Properties().apply {
            setProperty("beskrivelse", beskrivelse)
            setProperty("frist", frist.toString())
        }
        taskRepository.save(Task(type = OppdaterOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterEnhetOppgaveTask(behandlingId: UUID,
                                 beskrivelse: String,
                                 enhetId: String) {
        val properties = Properties().apply {
            setProperty("beskrivelse", beskrivelse)
            setProperty("enhetId", enhetId)
        }
        taskRepository.save(Task(type = OppdaterEnhetOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId: UUID) {
        val properties = Properties()
        taskRepository.save(Task(type = OppdaterAnsvarligSaksbehandlerTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }
}
