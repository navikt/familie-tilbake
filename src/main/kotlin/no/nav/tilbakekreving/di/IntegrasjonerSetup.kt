package no.nav.tilbakekreving.di

import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.nav.tilbakekreving.integrasjoner.dokument.saf.SafClient
import no.nav.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import no.nav.tilbakekreving.integrasjoner.norg2.Norg2Client
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import no.nav.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
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

    @Bean
    fun safClient(tokenExchangeService: TokenExchangeService): SafClient {
        return SafClient.opprett(applicationProperties.saf, tokenExchangeService)
    }

    @Bean
    fun norg2Client(tokenExchangeService: TokenExchangeService): Norg2Client {
        return Norg2Client.opprett(applicationProperties.norg2, tokenExchangeService)
    }

    @Bean
    fun eregClient(tokenExchangeService: TokenExchangeService): EregClient {
        return EregClient.opprett(applicationProperties.eregServices, tokenExchangeService)
    }

    @Bean
    fun entraProxyClient(tokenExchangeService: TokenExchangeService): EntraProxyClient {
        return EntraProxyClient.opprett(applicationProperties.entraProxy, tokenExchangeService)
    }

    @Bean
    fun pdfGenClient(): PdfGenClient {
        return PdfGenClient.opprett(applicationProperties.tilbakekrevingPdf)
    }

    @Bean
    fun oppdragRestClient(tokenExchangeService: TokenExchangeService): OppdragRestClient {
        return OppdragRestClient.opprett(applicationProperties.sokosOs, tokenExchangeService)
    }
}
