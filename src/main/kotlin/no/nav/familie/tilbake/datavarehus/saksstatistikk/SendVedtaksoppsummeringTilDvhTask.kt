package no.nav.familie.tilbake.datavarehus.saksstatistikk

import jakarta.validation.Validation
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVedtaksoppsummeringTilDvhTask.TYPE,
    beskrivelse = "Sender oppsummering av vedtak til datavarehus.",
)
class SendVedtaksoppsummeringTilDvhTask(
    private val vedtaksoppsummeringService: VedtaksoppsummeringService,
    private val kafkaProducer: KafkaProducer,
    private val logService: LogService,
) : AsyncTaskStep {
    private val validator = Validation.buildDefaultValidatorFactory().validator
    private val log = TracedLogger.getLogger<SendVedtaksoppsummeringTilDvhTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("SendVedtaksoppsummeringTilDvhTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }
        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandlingId)
        validate(vedtaksoppsummering)

        SecureLog.medContext(logContext) {
            info(
                "Sender Vedtaksoppsummering=${objectMapper.writeValueAsString(vedtaksoppsummering)} til Dvh " +
                    "for behandling $behandlingId",
            )
        }
        kafkaProducer.sendVedtaksdata(behandlingId, vedtaksoppsummering, logContext)
    }

    private fun validate(vedtaksoppsummering: Vedtaksoppsummering) {
        val valideringsfeil = validator.validate(vedtaksoppsummering)
        require(valideringsfeil.isEmpty()) {
            "Valideringsfeil for ${vedtaksoppsummering::class.simpleName}: Valideringsfeil:$valideringsfeil"
        }
    }

    companion object {
        const val TYPE = "dvh.send.vedtak"
    }
}
