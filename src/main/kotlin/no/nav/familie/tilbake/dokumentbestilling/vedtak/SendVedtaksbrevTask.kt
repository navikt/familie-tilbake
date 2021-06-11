package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = SendVedtaksbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender vedtaksbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class SendVedtaksbrevTask(private val behandlingRepository: BehandlingRepository,
                          private val vedtaksbrevService: VedtaksbrevService,
                          private val taskService: TaskService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.harVerge) {
            vedtaksbrevService.sendVedtaksbrev(behandling, Brevmottager.VERGE)
        }
        vedtaksbrevService.sendVedtaksbrev(behandling, Brevmottager.BRUKER)
        log.info("Utført for behandling: {}", behandlingId)
    }

    companion object {

        const val TYPE = "iverksetteVedtak.sendVedtaksbrev"
    }
}
