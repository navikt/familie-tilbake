package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.datavarehus.saksstatistikk.SendVedtaksoppsummeringTilDvhTask
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVedtaksbrevTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Sender vedtaksbrev",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class SendVedtaksbrevTask(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val vedtaksbrevService: VedtaksbrevService,
    private val taskService: TracableTaskService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<SendVedtaksbrevTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandlingId.toString())
        if (behandling.saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP) {
            log.medContext(logContext) {
                info("Behandlingen $behandlingId ble saksbehandlet automatisk, sender ikke vedtaksbrev")
            }
            taskService.save(
                Task(
                    type = AvsluttBehandlingTask.TYPE,
                    payload = task.payload,
                    properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsak.fagsystem.name) },
                ),
                task.logContext(),
            )
            return
        }

        if (behandling.type == Behandlingstype.REVURDERING_TILBAKEKREVING &&
            behandling.sisteÅrsak?.type in setOf(Behandlingsårsakstype.REVURDERING_KLAGE_KA)
        ) {
            log.medContext(logContext) {
                info("Sender ikke vedtaksbrev etter revurdering som følge av klage for behandling: {}", behandlingId)
            }
            taskService.save(
                Task(
                    type = AvsluttBehandlingTask.TYPE,
                    payload = task.payload,
                    properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsak.fagsystem.name) },
                ),
                task.logContext(),
            )
            return
        }

        vedtaksbrevService.sendVedtaksbrev(behandling)
        log.medContext(logContext) {
            info("Utført for behandling: {}", behandlingId)
        }
    }

    override fun onCompletion(task: Task) {
        taskService.save(
            Task(
                type = SendVedtaksoppsummeringTilDvhTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ),
            task.logContext(),
        )
    }

    companion object {
        const val TYPE = "iverksetteVedtak.sendVedtaksbrev"
    }
}
