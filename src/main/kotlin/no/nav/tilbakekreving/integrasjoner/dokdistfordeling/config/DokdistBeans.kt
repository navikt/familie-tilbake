package no.nav.tilbakekreving.integrasjoner.dokdistfordeling.config

import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClientImpl
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClientStub
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class DokdistBeans(
    private val props: ApplicationProperties,
) {
    @Bean("dokdistClient")
    @Profile("dev", "prod")
    fun dokdistClient(
        tokenExchangeService: TokenExchangeService,
    ): DokdistClient =
        DokdistClientImpl(
            applicationProperties = props,
            tokenExchangeService = tokenExchangeService,
        )

    @Bean("dokdistClient")
    @Profile("e2e", "local", "integrasjonstest")
    fun dokdistClientStub(): DokdistClient =
        DokdistClientStub()
}
