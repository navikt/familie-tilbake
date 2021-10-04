package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.SendVedtaksoppsummeringTilDvhTask
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = SendVedtaksbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender vedtaksbrev",
                     triggerTidVedFeilISekunder = 60 * 5L)
class SendVedtaksbrevTask(private val behandlingRepository: BehandlingRepository,
                          private val vedtaksbrevService: VedtaksbrevService,
                          private val taskService: TaskService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP) {
            log.info("Behandlingen $behandlingId ble saksbehandlet automatisk, sender ikke vedtaksbrev")
            taskService.save(Task(type = AvsluttBehandlingTask.TYPE,
                                  payload = task.payload))
            return
        }
        if (behandling.harVerge) {
            vedtaksbrevService.sendVedtaksbrev(behandling, Brevmottager.VERGE)
        }
        vedtaksbrevService.sendVedtaksbrev(behandling, Brevmottager.BRUKER)
        log.info("Utført for behandling: {}", behandlingId)
    }

    override fun onCompletion(task: Task) {
        taskService.save(Task(type = SendVedtaksoppsummeringTilDvhTask.TYPE,
                              payload = task.payload))
    }

    companion object {

        const val TYPE = "iverksetteVedtak.sendVedtaksbrev"
    }
}
