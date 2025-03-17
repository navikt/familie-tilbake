package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.SendVedtaksoppsummeringTilDvhTask
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import org.springframework.stereotype.Service
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
    private val vedtaksbrevService: VedtaksbrevService,
    private val taskService: TracableTaskService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<SendVedtaksbrevTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = task.logContext()
        if (behandling.saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP) {
            log.medContext(logContext) {
                info("Behandlingen $behandlingId ble saksbehandlet automatisk, sender ikke vedtaksbrev")
            }
            behandlingService.avslutt(behandlingId, logContext)
            return
        }

        if (behandling.type == Behandlingstype.REVURDERING_TILBAKEKREVING &&
            behandling.sisteÅrsak?.type in setOf(Behandlingsårsakstype.REVURDERING_KLAGE_KA)
        ) {
            log.medContext(logContext) {
                info("Sender ikke vedtaksbrev etter revurdering som følge av klage for behandling: {}", behandlingId)
            }
            behandlingService.avslutt(behandlingId, logContext)
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
