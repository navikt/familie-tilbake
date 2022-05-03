package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.PropertyName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(private val taskRepository: TaskRepository,
                         private val fagsakService: FagsakService) {

    @Transactional
    fun opprettOppgaveTask(behandling: Behandling, oppgavetype: Oppgavetype, saksbehandler: String? = null) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandling.id)
        val properties = Properties().apply {
            setProperty("oppgavetype", oppgavetype.name)
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            setProperty(PropertyName.ENHET, behandling.behandlendeEnhet)
            saksbehandler?.let { setProperty("saksbehandler", it) }
        }
        taskRepository.save(Task(type = LagOppgaveTask.TYPE,
                                 payload = behandling.id.toString(),
                                 properties = properties))
    }

    @Transactional
    fun ferdigstilleOppgaveTask(behandlingId: UUID, oppgavetype: String? = null) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
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
    fun oppdaterOppgaveTask(behandlingId: UUID, beskrivelse: String, frist: LocalDate, saksbehandler: String? = null) {
        opprettOppdaterOppgaveTask(behandlingId = behandlingId,
                                   beskrivelse = beskrivelse,
                                   frist = frist,
                                   saksbehandler = saksbehandler)
    }

    @Transactional
    fun oppdaterOppgaveTaskMedTriggertid(behandlingId: UUID, beskrivelse: String, frist: LocalDate, triggerTid: Long, saksbehandler: String? = null) {
        opprettOppdaterOppgaveTask(behandlingId = behandlingId,
                                   beskrivelse = beskrivelse,
                                   frist = frist,
                                   triggerTid = triggerTid,
                                   saksbehandler = saksbehandler)
    }

    private fun opprettOppdaterOppgaveTask(behandlingId: UUID, beskrivelse: String, frist: LocalDate, triggerTid: Long? = null, saksbehandler: String? = null) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties = Properties().apply {
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            setProperty("beskrivelse", beskrivelse)
            setProperty("frist", frist.toString())
            saksbehandler?.let { setProperty("saksbehandler", it) }
        }
        val task = Task(type = OppdaterOppgaveTask.TYPE,
                        payload = behandlingId.toString(),
                        properties = properties)
        triggerTid?.let { task.medTriggerTid(LocalDateTime.now().plusSeconds(it)) }
        taskRepository.save(task)
    }

    @Transactional
    fun oppdaterEnhetOppgaveTask(behandlingId: UUID, beskrivelse: String, enhetId: String) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties = Properties().apply {
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            setProperty("beskrivelse", beskrivelse)
            setProperty("enhetId", enhetId)
            setProperty("saksbehandler", ContextService.hentSaksbehandler())
        }
        taskRepository.save(Task(type = OppdaterEnhetOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId: UUID) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties = Properties().apply {
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
        }
        taskRepository.save(Task(type = OppdaterAnsvarligSaksbehandlerOppgaveTask.TYPE,
                                 payload = behandlingId.toString(),
                                 properties = properties))
    }
}
