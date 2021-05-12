package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
}
