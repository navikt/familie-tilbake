package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.kontrakter.Applikasjon
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.EmbeddedKafkaZKBroker

@Configuration
@EnableKafka
@Profile("local")
class KafkaLokalConfig(
    @Value("\${LOKAL_BROKER_KAFKA_PORT:8093}") private val brokerKafkaPort: Int,
    @Value("\${LOKAL_BROKER_REMOTE_PORT:8094}") private val brokerRemotePort: Int,
    @Value("\${kafka.hentFagsystem.requestTopic}")
    private val fagsystemsbehandlingRequestTopic: String,
    @Value("\${kafka.hentFagsystem.responseTopic}")
    private val fagsystemsbehandlingResponseTopic: String,
) {
    @Bean
    fun broker(): EmbeddedKafkaBroker {
        val propertyMap =
            mapOf(
                "listeners" to "PLAINTEXT://localhost:$brokerKafkaPort,REMOTE://localhost:$brokerRemotePort",
                "advertised.listeners" to "PLAINTEXT://localhost:$brokerKafkaPort,REMOTE://localhost:$brokerRemotePort",
                "listener.security.protocol.map" to "PLAINTEXT:PLAINTEXT,REMOTE:PLAINTEXT",
            )

        return EmbeddedKafkaZKBroker(1)
            // For å teste historikkinnslag, må EmbeddedKafkaBroker kjøre på port 8093
            // For å teste opprett behandling manuelt, må EmbeddedKafkaBroker kjøre på port 9092
            .kafkaPorts(brokerKafkaPort)
            .brokerProperties(propertyMap)
            .brokerListProperty("spring.kafka.bootstrap-servers")
    }

    @Bean
    fun hentFagsystemsbehandlingRequestTopic(): NewTopic =
        TopicBuilder
            .name(fagsystemsbehandlingRequestTopic)
            .partitions(1)
            .replicas(1)
            .build()

    @Bean
    fun hentFagsystemsbehandlingResponsTopic(): NewTopic =
        TopicBuilder
            .name(fagsystemsbehandlingResponseTopic)
            .partitions(1)
            .replicas(1)
            .build()

    @Bean
    fun producerFactory(): ProducerFactory<String, String> = DefaultKafkaProducerFactory(producerConfigs())

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> = KafkaTemplate(producerFactory())

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> = DefaultKafkaConsumerFactory(consumerConfigs())

    @Bean
    fun concurrentKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConcurrency(1)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.consumerFactory = consumerFactory()
        return factory
    }

    private fun producerConfigs() =
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            // Den sikrer rekkefølge
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            // Den sikrer at data ikke mistes
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.CLIENT_ID_CONFIG to Applikasjon.FAMILIE_TILBAKE.name,
        )

    private fun consumerConfigs() =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to "familie-tilbake",
            ConsumerConfig.CLIENT_ID_CONFIG to "consumer-familie-tilbake-1",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            CommonClientConfigs.RETRIES_CONFIG to 10,
            CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG to 100,
        )
}
