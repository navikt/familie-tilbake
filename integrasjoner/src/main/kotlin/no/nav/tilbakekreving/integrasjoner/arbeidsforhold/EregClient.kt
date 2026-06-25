package no.nav.tilbakekreving.integrasjoner.arbeidsforhold

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.HentOrganisasjonResponse
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

interface EregClient {
    fun hentOrganisasjon(orgnr: String): HentOrganisasjonResponse

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): EregClientImpl {
            return EregClientImpl(
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
