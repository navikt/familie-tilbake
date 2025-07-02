package no.nav.familie.tilbake

import no.nav.familie.tilbake.integration.kafka.KafkaProperties
import no.nav.tilbakekreving.config.ApplicationProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.jms.annotation.EnableJms

@EnableConfigurationProperties(KafkaProperties::class, ApplicationProperties::class)
@SpringBootApplication
@EnableJms
class Launcher

fun main(args: Array<String>) {
    SpringApplication.run(Launcher::class.java, *args)
}
