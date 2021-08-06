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
    fun opprettOppgaveTask(behandlingId: UUID, oppgavetype: Oppgavetype) {
        val properties = Properties()
        properties.setProperty("oppgavetype", oppgavetype.name)
        taskRepository.save(Task(type = LagOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun ferdigstilleOppgaveTask(behandlingId: UUID, oppgavetype: Oppgavetype) {
        val properties = Properties()
        properties.setProperty("oppgavetype", oppgavetype.name)
        taskRepository.save(Task(type = FerdigstillOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterOppgaveTask(behandlingId: UUID,
                            oppgavetype: Oppgavetype,
                            beskrivelse: String,
                            frist: LocalDate) {
        val properties = Properties().apply {
            setProperty("oppgavetype", oppgavetype.name)
            setProperty("beskrivelse", beskrivelse)
            setProperty("frist", frist.toString())
        }
        taskRepository.save(Task(type = OppdaterOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterEnhetOppgaveTask(behandlingId: UUID,
                                 oppgavetype: Oppgavetype,
                                 beskrivelse: String,
                                 enhetId: String) {
        val properties = Properties().apply {
            setProperty("oppgavetype", oppgavetype.name)
            setProperty("beskrivelse", beskrivelse)
            setProperty("enhetId", enhetId)
        }
        taskRepository.save(Task(type = OppdaterEnhetOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }
}
