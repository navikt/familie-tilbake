package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = SendVedtaksbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender vedtaksbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class SendVedtaksbrevTask(private val behandlingRepository: BehandlingRepository,
                          private val vedtaksbrevTjeneste: VedtaksbrevService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.harVerge) {
            vedtaksbrevTjeneste.sendVedtaksbrev(behandling, Brevmottager.VERGE)
        }
        vedtaksbrevTjeneste.sendVedtaksbrev(behandling, Brevmottager.BRUKER)
        log.info("Utført for behandling: {}", behandlingId)
    }

    companion object {

        const val TYPE = "iverksetteVedtak.sendVedtaksbrev"
    }
}