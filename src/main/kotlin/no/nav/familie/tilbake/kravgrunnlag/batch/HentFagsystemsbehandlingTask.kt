package no.nav.familie.tilbake.kravgrunnlag.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HentFagsystemsbehandlingTask.TYPE,
    beskrivelse = "Sender kafka request til fagsystem for å hente behandling data",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class HentFagsystemsbehandlingTask(
    private val gammelKravgrunnlagService: GammelKravgrunnlagService,
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
    private val taskService: TracableTaskService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<HentFagsystemsbehandlingTask>()

    override fun doTask(task: Task) {
        log.medContext(task.logContext()) { info("HentFagsystemsbehandlingTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString()) }
        val mottattXmlId = UUID.fromString(task.payload)
        val mottattXml = gammelKravgrunnlagService.hentFrakobletKravgrunnlag(mottattXmlId)
        task.metadata["eksternFagsakId"] = mottattXml.eksternFagsakId

        gammelKravgrunnlagService.sjekkOmDetFinnesEnAktivBehandling(mottattXml)
        hentFagsystemsbehandlingService.sendHentFagsystemsbehandlingRequest(
            eksternFagsakId = mottattXml.eksternFagsakId,
            ytelsestype = mottattXml.ytelsestype,
            eksternId = mottattXml.referanse,
        )
    }

    @Transactional
    override fun onCompletion(task: Task) {
        log.medContext(task.logContext()) { info("Oppretter GammelKravgrunnlagTask for mottattXmlId={}", task.payload) }
        taskService.save(
            Task(
                type = GammelKravgrunnlagTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ).medTriggerTid(LocalDateTime.now().plusSeconds(60)),
            task.logContext(),
        )
    }

    companion object {
        const val TYPE = "gammelKravgrunnlag.hentFagsystemsbehandling"
    }
}
