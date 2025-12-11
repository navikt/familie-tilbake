package no.nav.tilbakekreving.integrasjoner.azure.config

import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.azure.AzureGraphClient
import no.nav.tilbakekreving.integrasjoner.azure.AzureGraphClientImpl
import no.nav.tilbakekreving.integrasjoner.azure.AzureGraphClientStub
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class AzureGraphBeans (
    private val props: ApplicationProperties
) {

    @Bean("azureGraphClient")
    @Profile("dev", "prod")
    fun azureGraphClient(): AzureGraphClient = AzureGraphClientImpl()

    @Bean("azureGraphClient")
    @Profile("e2e", "local", "integrasjonstest")
    fun azureGraphClientStub(): AzureGraphClient = AzureGraphClientStub()
}