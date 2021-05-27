package no.nav.familie.tilbake.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker


@Configuration
@EnableKafka
@Profile("local")
class KafkaLokalConfig {

    @Bean
    fun broker(): EmbeddedKafkaBroker {
        return EmbeddedKafkaBroker(1)
                .kafkaPorts(8093) // Det bør kjøre på en annen port enn 9092(brukes for historikkinnslag)
                .brokerProperty("listeners", "PLAINTEXT://localhost:8093,REMOTE://localhost:8094")
                .brokerProperty("advertised.listeners", "PLAINTEXT://localhost:8093,REMOTE://localhost:8094")
                .brokerProperty("listener.security.protocol.map", "PLAINTEXT:PLAINTEXT,REMOTE:PLAINTEXT")
                .brokerListProperty("spring.kafka.bootstrap-servers")
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    private fun producerConfigs() = mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
                                          ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                                          ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java)

}
