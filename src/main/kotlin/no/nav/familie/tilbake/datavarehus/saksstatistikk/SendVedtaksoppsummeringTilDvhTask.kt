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
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(this::class.java)
    private val validator = Validation.buildDefaultValidatorFactory().validator

    override fun doTask(task: Task) {
        log.info("SendVedtaksoppsummeringTilDvhTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandlingId)
        val logContext = logService.contextFraBehandling(behandlingId)
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
