package no.nav.tilbakekreving.integrasjoner.entraProxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.tilbakekreving.integrasjoner.entraProxy.kontrakter.Saksbehandler
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

interface EntraProxyClient {
    fun hentSaksbehandler(id: String): Saksbehandler

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): EntraProxyClientImpl {
            return EntraProxyClientImpl(
                config = config,
                tokenExchangeService = tokenExchangeService,
                httpClient = HttpClient(Apache5) {
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
