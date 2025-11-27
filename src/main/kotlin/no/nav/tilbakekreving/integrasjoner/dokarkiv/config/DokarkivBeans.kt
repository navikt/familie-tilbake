package no.nav.tilbakekreving.integrasjoner.dokarkiv.config

import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClientImpl
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClientStub
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class DokarkivBeans(
    private val props: ApplicationProperties,
) {
    @Bean(name = ["dokarkivClient"])
    @Profile("dev", "prod")
    fun dokarkivClient(
        tokenExchangeService: TokenExchangeService,
    ): DokarkivClient =
        DokarkivClientImpl(
            applicationProperties = props,
            tokenExchangeService = tokenExchangeService,
        )

    @Bean(name = ["dokarkivClient"])
    @Profile("e2e", "local", "integrasjonstest")
    fun dokarkivClientStub(): DokarkivClient =
        DokarkivClientStub()
}
