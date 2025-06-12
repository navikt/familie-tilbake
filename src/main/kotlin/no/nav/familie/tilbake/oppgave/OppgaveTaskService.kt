package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class OppgaveTaskService(
    private val taskService: TracableTaskService,
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val logService: LogService,
) {
    @Transactional
    fun opprettOppgaveTask(
        behandling: Behandling,
        oppgavetype: Oppgavetype,
        saksbehandler: String? = null,
        opprettetAv: String? = null,
        beskrivelse: String? = null,
        logContext: SecureLog.Context,
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
            logContext,
        )
    }

    @Transactional
    fun ferdigstilleOppgaveTask(
        behandlingId: UUID,
        oppgavetype: String? = null,
    ) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val logContext = logService.contextFraBehandling(behandlingId)
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
            logContext,
        )
    }

    @Transactional
    fun oppdaterOppgaveTask(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        saksbehandler: String? = null,
        logContext: SecureLog.Context,
    ) {
        opprettOppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = beskrivelse,
            frist = frist,
            saksbehandler = saksbehandler,
            logContext = logContext,
        )
    }

    @Transactional
    fun oppdaterOppgaveTaskMedTriggertid(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        triggerTid: Long,
        saksbehandler: String? = null,
        logContext: SecureLog.Context,
    ) {
        opprettOppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = beskrivelse,
            frist = frist,
            triggerTid = triggerTid,
            saksbehandler = saksbehandler,
            logContext = logContext,
        )
    }

    private fun opprettOppdaterOppgaveTask(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        triggerTid: Long? = null,
        saksbehandler: String? = null,
        logContext: SecureLog.Context,
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
        taskService.save(task, logContext)
    }

    @Transactional
    fun oppdaterAnsvarligSaksbehandlerOppgaveTask(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
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
            logContext,
        )
    }

    fun oppdaterOppgavePrioritetTask(
        behandlingId: UUID,
        fagsakId: String,
        logContext: SecureLog.Context,
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
            logContext,
        )
    }

    @Transactional
    fun opprettFinnGammelBehandlingUtenOppgaveTask(fagsystem: Fagsystem) {
        taskService.save(
            Task(
                type = FinnGammelBehandlingUtenOppgaveTask.TYPE,
                payload = objectMapper.writeValueAsString(FinnGammelBehandlingUtenOppgaveTask.FinnGammelBehandlingUtenOppgaveDto(fagsystem)),
            ),
            SecureLog.Context.tom(),
        )
    }

    @Transactional
    fun ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(
        behandlingId: UUID,
        beskrivelse: String,
        frist: LocalDate,
        logContext: SecureLog.Context,
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
            logContext,
        )
    }

    @Transactional
    fun finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave(fagsystem: Fagsystem) {
        taskService.save(
            Task(
                type = FinnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgaveTask.TYPE,
                payload = fagsystem.name,
            ),
            SecureLog.Context.tom(),
        )
    }
}
