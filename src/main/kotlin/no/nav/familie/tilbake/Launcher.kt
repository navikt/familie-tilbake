package no.nav.familie.tilbake

import no.nav.familie.tilbake.config.ApplicationConfig
import no.nav.familie.tilbake.integration.kafka.KafkaProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.tilbakekreving.config.ApplicationProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan

@ConfigurationPropertiesScan
@ComponentScan(ApplicationConfig.PAKKE_NAVN, "no.nav.familie.prosessering", "no.nav.tilbakekreving")
@EnableConfigurationProperties(KafkaProperties::class, ApplicationProperties::class)
@EnableOAuth2Client(cacheEnabled = true)
@SpringBootApplication
class Launcher

fun main(args: Array<String>) {
    SpringApplication.run(Launcher::class.java, *args)
}
