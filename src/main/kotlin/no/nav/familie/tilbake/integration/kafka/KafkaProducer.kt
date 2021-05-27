package no.nav.familie.tilbake.integration.kafka

import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun sendHistorikkinnslag(behandlingId: UUID, key: String, request: OpprettHistorikkinnslagRequest) {
        val melding = objectMapper.writeValueAsString(request)
        val producerRecord = ProducerRecord(Constants.historikkTopic, key, melding)
        kafkaTemplate.send(producerRecord)
                .addCallback({
                                 log.info("Historikkinnslag for $behandlingId med $key er sendt. " +
                                          "Fikk offset ${it?.recordMetadata?.offset()}")
                             },
                             {
                                 log.warn("Historikkinnslag kan ikke sendes for $behandlingId med $key. Feiler med ${it.message}")
                                 throw Feil(message = "Historikkinnslag kan ikke sendes for $behandlingId med $key. " +
                                                      "Feiler med ${it.message}", it)
                             })
    }
}
