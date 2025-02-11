package no.nav.familie.tilbake.iverksettvedtak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.iverksettvedtak.IverksettelseService
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
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
    private val taskService: TaskService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<SendØkonomiTilbakekrevingsvedtakTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("SendØkonomiTilbakekrevingsvedtakTask prosesserer med id=${task.id} og metadata ${task.metadata}")
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
        )
    }

    companion object {
        const val TYPE = "sendØkonomiVedtak"
    }
}
