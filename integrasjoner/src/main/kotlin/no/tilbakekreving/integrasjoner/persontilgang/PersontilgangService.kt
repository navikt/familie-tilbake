package no.tilbakekreving.integrasjoner.persontilgang

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.tilgangsmaskinen.TilgangsmaskinenClient
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

interface PersontilgangService {
    suspend fun sjekkPersontilgang(
        callContext: CallContext.Saksbehandler,
        personIdent: String,
    ): Persontilgang

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): PersontilgangService {
            return PersontilgangServiceImpl(
                client = TilgangsmaskinenClient(
                    config,
                    HttpClient(Apache) {
                        install(ContentNegotiation) {
                            jackson {
                                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            }
                        }
                    },
                    tokenExchangeService,
                ),
            )
        }

        data class Config(
            val baseUrl: String,
            val scope: String,
        )
    }
}
