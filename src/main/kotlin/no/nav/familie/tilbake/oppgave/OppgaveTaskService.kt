package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
) {
    @Transactional
    fun opprettOppgaveTask(
        behandling: Behandling,
        oppgavetype: Oppgavetype,
        saksbehandler: String? = null,
        opprettetAv: String? = null,
        beskrivelse: String? = null,
    ) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandling.id)
        val properties =
            Properties().apply {
                setProperty("oppgavetype", oppgavetype.name)
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
                setProperty(PropertyName.ENHET, behandling.behandlendeEnhet)
                saksbehandler?.let { setProperty("saksbehandler", it) }
                opprettetAv?.let { setProperty("opprettetAv", it) }
                beskrivelse?.let { setProperty("beskrivelse", it) }
            }
        taskService.save(
            Task(
                type = LagOppgaveTask.TYPE,
                payload = behandling.id.toString(),
                properties = properties,
            ),
        )
    }

    @Transactional
    fun ferdigstilleOppgaveTask(
        behandlingId: UUID,
        oppgavetype: String? = null,
    ) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties =
            Properties().apply {
                if (!oppgavetype.isNullOrEmpty()) {
                    setProperty("oppgavetype", oppgavetype)
                }
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            }
        taskService.save(
            Task(
                type = FerdigstillOppgaveTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            ),
        )
    }

    @Transactional
    fun oppdaterOppgaveTask(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        saksbehandler: String? = null,
    ) {
        opprettOppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = beskrivelse,
            frist = frist,
            saksbehandler = saksbehandler,
        )
    }

    @Transactional
    fun oppdaterOppgaveTaskMedTriggertid(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        triggerTid: Long,
        saksbehandler: String? = null,
    ) {
        opprettOppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = beskrivelse,
            frist = frist,
            triggerTid = triggerTid,
            saksbehandler = saksbehandler,
        )
    }

    private fun opprettOppdaterOppgaveTask(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        triggerTid: Long? = null,
        saksbehandler: String? = null,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties =
            Properties().apply {
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
                setProperty("beskrivelse", beskrivelse)
                setProperty("frist", frist.toString())
                setProperty("enhet", behandling.behandlendeEnhet)
                saksbehandler?.let { setProperty("saksbehandler", it) }
            }
        val task =
            Task(
                type = OppdaterOppgaveTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            )
        triggerTid?.let { task.medTriggerTid(LocalDateTime.now().plusSeconds(it)) }
        taskService.save(task)
    }

    @Transactional
    fun oppdaterEnhetOppgaveTask(
        behandlingId: UUID,
        beskrivelse: String,
        enhetId: String,
    ) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties =
            Properties().apply {
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
                setProperty("beskrivelse", beskrivelse)
                setProperty("enhetId", enhetId)
                setProperty("saksbehandler", ContextService.hentSaksbehandler())
                setProperty("behandlingId", behandlingId.toString())
            }
        taskService.save(
            Task(
                type = OppdaterEnhetOppgaveTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            ),
        )
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId: UUID) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties =
            Properties().apply {
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            }
        taskService.save(
            Task(
                type = OppdaterAnsvarligSaksbehandlerTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            ),
        )
    }

    fun oppdaterOppgavePrioritetTask(
        behandlingId: UUID,
        fagsakId: String,
    ) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties =
            Properties().apply {
                setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
                setProperty("behandlingId", behandlingId.toString())
                setProperty("ekstertFagsakId", fagsakId)
            }
        taskService.save(
            Task(
                type = OppdaterPrioritetTask.TYPE,
                payload = behandlingId.toString(),
                properties = properties,
            ),
        )
    }

    @Transactional
    fun opprettFinnGammelBehandlingUtenOppgaveTask(
        fagsystem: Fagsystem,
    ) {
        taskService.save(
            Task(
                type = FinnGammelBehandlingUtenOppgaveTask.TYPE,
                payload = objectMapper.writeValueAsString(FinnGammelBehandlingUtenOppgaveTask.FinnGammelBehandlingUtenOppgaveDto(fagsystem)),
            ),
        )
    }

    @Transactional
    fun ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
    ) {
        taskService.save(
            Task(
                type = FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveDto(
                            behandlingId = behandlingId,
                            beskrivelse = beskrivelse,
                            frist = frist,
                        ),
                    ),
            ),
        )
    }

    @Transactional
    fun finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave(fagsystem: Fagsystem) {
        taskService.save(
            Task(
                type = FinnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgaveTask.TYPE,
                payload = fagsystem.name,
            ),
        )
    }
}
