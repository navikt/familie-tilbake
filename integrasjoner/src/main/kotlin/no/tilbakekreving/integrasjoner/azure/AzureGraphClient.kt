package no.tilbakekreving.integrasjoner.azure

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.tilbakekreving.integrasjoner.azure.kontrakter.AzureAdBruker
import no.tilbakekreving.integrasjoner.azure.kontrakter.AzureAdBrukere
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

interface AzureGraphClient {
    fun finnSaksbehandler(navIdent: String): AzureAdBrukere

    fun hentSaksbehandler(id: String): AzureAdBruker

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): AzureGraphClientImpl {
            return AzureGraphClientImpl(
                config = config,
                tokenExchangeService = tokenExchangeService,
                httpClient = HttpClient(Apache) {
                    install(ContentNegotiation) {
                        jackson()
                    }
                },
            )
        }

        data class Config(
            val baseUrl: String,
            val scope: String,
        )
    }
}
