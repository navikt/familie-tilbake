package no.nav.tilbakekreving.integrasjoner.persontilgang

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.tilbakekreving.integrasjoner.CallContext
import no.nav.tilbakekreving.integrasjoner.tilgangsmaskinen.TilgangsmaskinenClient
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

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
                    HttpClient(Apache5) {
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
