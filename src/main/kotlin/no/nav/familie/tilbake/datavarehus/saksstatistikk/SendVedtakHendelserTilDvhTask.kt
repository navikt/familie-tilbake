package no.nav.familie.tilbake.datavarehus.saksstatistikk

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.springframework.stereotype.Service
import java.util.UUID
import javax.validation.Validation

@Service
@TaskStepBeskrivelse(taskStepType = SendVedtakHendelserTilDvhTask.TYPE,
                     beskrivelse = "Sender oppsummering av vedtak til datavarehus.")
class SendVedtakHendelserTilDvhTask(private val vedtaksoppsummeringService: VedtaksoppsummeringService,
                                    private val kafkaProducer: KafkaProducer) : AsyncTaskStep {

    private val validator = Validation.buildDefaultValidatorFactory().validator


    override fun doTask(task: Task) {

        val behandlingId = UUID.fromString(task.metadata.getProperty("behandlingId"))
        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandlingId)
        validate(vedtaksoppsummering)
        kafkaProducer.sendVedtaksdata(behandlingId, vedtaksoppsummering)
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