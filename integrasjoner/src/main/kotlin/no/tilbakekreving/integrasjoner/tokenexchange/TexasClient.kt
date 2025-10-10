package no.tilbakekreving.integrasjoner.tokenexchange

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import no.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenErrorResponse
import no.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenResponse

class TexasClient(
    private val httpClient: HttpClient,
    private val config: TokenExchangeService.Companion.Config,
) {
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
