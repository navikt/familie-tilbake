package no.nav.familie.tilbake.config

import no.nav.familie.kontrakter.felles.Applikasjon
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@Configuration
@EnableKafka
@Profile("preprod", "prod")
class KafkaConfig(@Value("\${KAFKA_BROKERS:localhost}") private val kafkaBrokers: String,
                  @Value("\${KAFKA_TRUSTSTORE_PATH}") private val kafkaTruststorePath: String,
                  @Value("\${KAFKA_CREDSTORE_PASSWORD}") private val kafkaCredstorePassword: String,
                  @Value("\${KAFKA_KEYSTORE_PATH}") private val kafkaKeystorePath: String) {

    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    private fun producerConfigs() =
            mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                  ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                  ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                  ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
                  ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
                  ProducerConfig.CLIENT_ID_CONFIG to Applikasjon.FAMILIE_TILBAKE.name) + securityConfig()

    private fun securityConfig() =
            mapOf(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
                  SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
                  SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
                  SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
                  SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
                  SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
                  SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
                  SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
                  SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword)

    companion object {

        val HISTORIKK_TOPIC = "teamfamilie.privat-historikk-topic"
        val HENT_FAGSYSTEMSBEHANDLING_REQUEST_TOPIC = "teamfamilie.privat-tbk-hentfagsystemsbehandling-request-topic"
        val HENT_FAGSYSTEMSBEHANDLING_RESPONS_TOPIC = "teamfamilie.privat-tbk-hentfagsystemsbehandling-respons-topic"
        val SAK_TOPIC = "teamfamilie.aapen-tbk-datavarehus-sak-topic"
        val VEDTAK_TOPIC = "teamfamilie.aapen-tbk-datavarehus-vedtak-topic"
    }
}
