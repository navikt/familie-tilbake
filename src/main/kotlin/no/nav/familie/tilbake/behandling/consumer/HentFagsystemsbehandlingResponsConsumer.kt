package no.nav.familie.tilbake.behandling.consumer

import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("!integrasjonstest & !e2e & !local")
class HentFagsystemsbehandlingResponsConsumer(
    private val fagsystemsbehandlingService: HentFagsystemsbehandlingService,
) {
    @KafkaListener(
        id = "familie-tilbake",
        topics = ["\${kafka.hentFagsystem.responseTopic}"],
        containerFactory = "concurrentKafkaListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        fagsystemsbehandlingService.lagreHentFagsystemsbehandlingRespons(UUID.fromString(consumerRecord.key()), consumerRecord.value())
        ack.acknowledge()
    }
}
