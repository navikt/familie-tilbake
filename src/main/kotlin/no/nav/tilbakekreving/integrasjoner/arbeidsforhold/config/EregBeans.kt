package no.nav.tilbakekreving.integrasjoner.arbeidsforhold.config

import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClientImpl
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClientStub
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class EregBeans(
    private val props: ApplicationProperties,
) {
    @Bean("eregClient")
    @Profile("dev", "prod")
    fun eregClient(
        tokenExchangeService: TokenExchangeService,
    ): EregClientImpl =
        EregClientImpl(
            applicationProperties = props,
            tokenExchangeService = tokenExchangeService,
        )

    @Bean("eregClient")
    @Profile("e2e", "local", "integrasjonstest")
    fun eregClientStub(): EregClientStub =
        EregClientStub()
}
