package no.nav.tilbakekreving.integrasjoner.dokumenthenting.config

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.SafClient
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.SafClientImpl
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.SafClientStub
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class SafBeans(
    private val props: ApplicationProperties,
) {
    @Bean("safClient")
    @Profile("dev", "prod")
    fun safClient(
        tokenExchangeService: TokenExchangeService,
        tokenValidationContextHolder: TokenValidationContextHolder,
    ): SafClient = SafClientImpl(
        applicationProperties = props,
        tokenExchangeService = tokenExchangeService,
        tokenValidationContextHolder = tokenValidationContextHolder,
    )

    @Bean("sakClient")
    @Profile("e2e", "local", "integrasjonstest")
    fun sakClientStub(): SafClient = SafClientStub()
}
