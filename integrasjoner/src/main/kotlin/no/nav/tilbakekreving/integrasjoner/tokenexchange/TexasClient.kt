package no.nav.tilbakekreving.integrasjoner.tokenexchange

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import no.nav.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenErrorResponse
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenIntrospectionResponse
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenResponse

class TexasClient(
    private val httpClient: HttpClient,
    private val config: TokenExchangeService.Companion.Config,
) {
    internal suspend fun introspectToken(token: String): TokenIntrospectionResponse {
        val response = httpClient.submitForm(
            config.tokenIntrospectionEndpoint,
            parameters {
                set("identity_provider", "entra_id")
                set("token", token)
            },
        ) {
            timeout {
                requestTimeoutMillis = 5_000
            }
        }
        if (!response.status.isSuccess()) {
            throw UnexpectedResponseException(
                message = "Token introspection failed",
                endpoint = "texas_token_introspection",
                statusCode = response.status,
                response = null,
            )
        }
        return response.body<TokenIntrospectionResponse>()
    }

    internal suspend fun exchangeToken(
        userToken: String,
        targetScope: String,
    ): TokenResponse {
        val response = httpClient
            .submitForm(
                config.tokenExchangeEndpoint,
                parameters {
                    set("target", targetScope)
                    set("user_token", userToken)
                    set("identity_provider", "azuread")
                },
            )
        return if (response.status.isSuccess()) {
            return response.body<TokenResponse.Success>()
        } else {
            TokenResponse.Error(response.body<TokenErrorResponse>(), response.status)
        }
    }

    internal suspend fun clientCredentialsToken(
        targetScope: String,
    ): TokenResponse {
        val response = httpClient
            .submitForm(
                config.tokenEndpoint,
                parameters {
                    set("target", targetScope)
                    set("identity_provider", "azuread")
                },
            )
        return if (response.status.isSuccess()) {
            return response.body<TokenResponse.Success>()
        } else {
            TokenResponse.Error(response.body<TokenErrorResponse>(), response.status)
        }
    }
}
