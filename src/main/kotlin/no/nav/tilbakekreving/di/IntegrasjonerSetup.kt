package no.nav.tilbakekreving.di

import no.nav.tilbakekreving.config.ApplicationProperties
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IntegrasjonerSetup(
    private val applicationProperties: ApplicationProperties,
) {
    @Bean
    fun tokenExchangeService(): TokenExchangeService {
        return TokenExchangeService.opprett(applicationProperties.tokenExchange)
    }

    @Bean
    fun personTilgangService(tokenExchangeService: TokenExchangeService): PersontilgangService {
        return PersontilgangService.opprett(applicationProperties.tilgangsmaskinen, tokenExchangeService)
    }
}
