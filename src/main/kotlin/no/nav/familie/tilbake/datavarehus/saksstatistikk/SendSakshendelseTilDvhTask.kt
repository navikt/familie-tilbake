package no.nav.familie.tilbake.datavarehus.saksstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendSakshendelseTilDvhTask.TASK_TYPE,
    beskrivelse = "Sending av sakshendelser til datavarehus",
)
class SendSakshendelseTilDvhTask(
    private val kafkaProducer: KafkaProducer,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<SendSakshendelseTilDvhTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("SendSakshendelseTilDvhTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }
        val behandlingstilstand: Behandlingstilstand = objectMapper.readValue(task.metadata.getProperty("behandlingstilstand"))
        kafkaProducer.sendSaksdata(
            behandlingId,
            behandlingstilstand.copy(tekniskTidspunkt = OffsetDateTime.now(ZoneOffset.UTC)),
            logContext,
        )
    }

    companion object {
        const val TASK_TYPE = "dvh.send.sakshendelse"
    }
}
