package no.nav.tilbakekreving.di

import no.nav.tilbakekreving.config.ApplicationProperties
import no.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.tilbakekreving.integrasjoner.dokument.saf.SafClient
import no.tilbakekreving.integrasjoner.norg2.Norg2Client
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

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

    @Bean
    fun safClient(tokenExchangeService: TokenExchangeService): SafClient {
        return SafClient.opprett(applicationProperties.saf, tokenExchangeService)
    }

    @Bean
    fun norg2Client(tokenExchangeService: TokenExchangeService): Norg2Client {
        return Norg2Client.opprett(applicationProperties.norg2, tokenExchangeService)
    }

    @Bean
    @Profile("dev", "prod")
    fun eregClient(tokenExchangeService: TokenExchangeService): EregClient {
        return EregClient.opprett(applicationProperties.eregServices, tokenExchangeService)
    }
}
