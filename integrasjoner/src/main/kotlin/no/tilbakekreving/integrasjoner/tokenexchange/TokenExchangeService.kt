package no.tilbakekreving.integrasjoner.tokenexchange

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson

interface TokenExchangeService {
    suspend fun onBehalfOfToken(
        userToken: String,
        targetScope: String,
    ): String

    companion object {
        fun opprett(
            config: Config,
        ): TokenExchangeService = TokenExchangeServiceImpl(
            texasClient = TexasClient(
                httpClient = HttpClient(Apache) {
                    install(ContentNegotiation) {
                        jackson {
                            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            registerModule(JavaTimeModule())
                        }
                    }
                },
                config = config,
            ),
        )

        data class Config(
            val tokenEndpoint: String,
            val tokenExchangeEndpoint: String,
            val tokenIntrospectionEndpoint: String,
        )
    }
}
