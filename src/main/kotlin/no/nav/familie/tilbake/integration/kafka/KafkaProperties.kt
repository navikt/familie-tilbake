package no.nav.familie.tilbake.integration.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka")
data class KafkaProperties(
    val hentFagsystem: HentFagsystem,
) {
    data class HentFagsystem(
        val requestTopic: String,
        val responseTopic: String,
    )
}
