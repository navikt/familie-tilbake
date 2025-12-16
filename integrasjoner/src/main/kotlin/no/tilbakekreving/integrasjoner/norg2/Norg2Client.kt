package no.tilbakekreving.integrasjoner.norg2

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.tilbakekreving.integrasjoner.norg2.kontrakter.NavKontorEnhet
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

interface Norg2Client {
    fun hentNavkontor(enhetId: String): NavKontorEnhet

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): Norg2Client {
            return Norg2ClientImpl(
                config = config,
                tokenExchangeService = tokenExchangeService,
                httpClient = HttpClient(Apache) {
                    install(ContentNegotiation) {
                        jackson {
                            registerModule(JavaTimeModule())
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        }
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
