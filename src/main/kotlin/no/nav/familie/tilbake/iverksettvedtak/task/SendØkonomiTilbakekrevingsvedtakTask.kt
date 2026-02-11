package no.nav.familie.tilbake.iverksettvedtak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.iverksettvedtak.IverksettelseService
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendØkonomiTilbakekrevingsvedtakTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Sender tilbakekrevingsvedtak til økonomi",
    triggerTidVedFeilISekunder = 300L,
)
class SendØkonomiTilbakekrevingsvedtakTask(
    private val iverksettelseService: IverksettelseService,
    private val taskService: TracableTaskService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<SendØkonomiTilbakekrevingsvedtakTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("SendØkonomiTilbakekrevingsvedtakTask prosesserer med id={}", task.id)
        }
        iverksettelseService.sendIverksettVedtak(behandlingId)

        behandlingskontrollService
            .oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.IVERKSETT_VEDTAK,
                    behandlingsstegstatus = Behandlingsstegstatus.UTFØRT,
                ),
                logContext,
            )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun onCompletion(task: Task) {
        taskService.save(
            Task(
                type = SendVedtaksbrevTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ),
            task.logContext(),
        )
    }

    companion object {
        const val TYPE = "sendØkonomiVedtak"
    }
}
