package no.nav.familie.tilbake.integration.kafka

import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.KafkaConfig
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun sendHistorikkinnslag(behandlingId: UUID, key: String, request: OpprettHistorikkinnslagRequest) {
        sendKafkamelding(behandlingId, KafkaConfig.HISTORIKK_TOPIC, key, request)
    }

    fun sendSaksdata(behandlingId: UUID, request: Behandlingstilstand) {
        sendKafkamelding(behandlingId, KafkaConfig.SAK_TOPIC, request.behandlingUuid.toString(), request)
    }

    fun sendVedtaksdata(behandlingId: UUID, request: Vedtaksoppsummering) {
        sendKafkamelding(behandlingId, KafkaConfig.VEDTAK_TOPIC, "error", request)
    }

    private fun sendKafkamelding(behandlingId: UUID, topic: String, key: String, request: Any) {
        val melding = objectMapper.writeValueAsString(request)
        val producerRecord = ProducerRecord(topic, key, melding)
        kafkaTemplate.send(producerRecord)
                .addCallback({
                                 log.info("Melding på topic $topic for $behandlingId med $key er sendt. " +
                                          "Fikk offset ${it?.recordMetadata?.offset()}")
                             },
                             {
                                 val feilmelding = "Melding på topic $topic kan ikke sendes for $behandlingId med $key. " +
                                                   "Feiler med ${it.message}"
                                 log.warn(feilmelding)
                                 throw Feil(message = feilmelding)
                             })
    }
}
