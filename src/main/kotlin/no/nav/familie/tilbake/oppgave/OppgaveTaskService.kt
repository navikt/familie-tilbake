package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.config.PropertyName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(private val taskRepository: TaskRepository) {

    @Transactional
    fun opprettOppgaveTask(behandlingId: UUID, fagsystem: Fagsystem, oppgavetype: Oppgavetype, saksbehandler: String? = null) {
        val properties = Properties().apply {
            setProperty("oppgavetype", oppgavetype.name)
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            saksbehandler?.let { setProperty("saksbehandler", it) }
        }
        taskRepository.save(Task(type = LagOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun ferdigstilleOppgaveTask(behandlingId: UUID, fagsystem: Fagsystem, oppgavetype: String? = null) {
        val properties = Properties().apply {
            if (!oppgavetype.isNullOrEmpty()) {
                setProperty("oppgavetype", oppgavetype)
            }
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
        }
        taskRepository.save(Task(type = FerdigstillOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterOppgaveTask(behandlingId: UUID, fagsystem: String, beskrivelse: String, frist: LocalDate) {
        val properties = Properties().apply {
            setProperty(PropertyName.FAGSYSTEM, fagsystem)
            setProperty("beskrivelse", beskrivelse)
            setProperty("frist", frist.toString())
        }
        taskRepository.save(Task(type = OppdaterOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterEnhetOppgaveTask(behandlingId: UUID, fagsystem: Fagsystem, beskrivelse: String, enhetId: String) {
        val properties = Properties().apply {
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            setProperty("beskrivelse", beskrivelse)
            setProperty("enhetId", enhetId)
        }
        taskRepository.save(Task(type = OppdaterEnhetOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId: UUID, fagsystem: Fagsystem) {
        val properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) }
        taskRepository.save(Task(type = OppdaterAnsvarligSaksbehandlerTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }
}
