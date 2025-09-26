package no.nav.tilbakekreving.config

import no.nav.tilbakekreving.fagsystem.FagsystemKafkaListener
import no.nav.tilbakekreving.fagsystem.Ytelse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer

@Profile("dev", "prod")
@Configuration
class KafkaListenerConfig {
    @Bean
    fun listeners(
        consumerFactory: DefaultKafkaConsumerFactory<String, String>,
        fagsystemKafkaListener: FagsystemKafkaListener,
    ): KafkaMessageListenerContainer<String?, String?> {
        val listenerContainer = KafkaMessageListenerContainer(
            consumerFactory,
            ContainerProperties(*Ytelse.ytelser().map { it.kafkaTopic }.toTypedArray()),
        )

        listenerContainer.setupMessageListener(fagsystemKafkaListener)
        listenerContainer.start()
        return listenerContainer
    }
}
