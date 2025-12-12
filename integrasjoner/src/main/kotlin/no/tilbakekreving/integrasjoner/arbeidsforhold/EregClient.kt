package no.tilbakekreving.integrasjoner.arbeidsforhold

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.HentOrganisasjonResponse
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

interface EregClient {
    fun hentOrganisasjon(orgnr: String): HentOrganisasjonResponse

    fun validerOrganisasjon(orgnr: String): Boolean

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): EregClientImpl {
            return EregClientImpl(
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
