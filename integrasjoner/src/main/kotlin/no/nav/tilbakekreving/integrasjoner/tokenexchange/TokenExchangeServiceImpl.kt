package no.nav.tilbakekreving.integrasjoner.tokenexchange

import no.nav.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenResponse

internal class TokenExchangeServiceImpl(
    private val texasClient: TexasClient,
) : TokenExchangeService {
    override suspend fun onBehalfOfToken(
        userToken: String,
        targetScope: String,
    ): String {
        return when (val response = texasClient.exchangeToken(userToken, targetScope)) {
            is TokenResponse.Success -> response.accessToken
            is TokenResponse.Error -> throw UnexpectedResponseException(
                message = "Kunne ikke autentisere mot $targetScope feilmelding: ${response.error.errorDescription}",
                endpoint = "texas_on_behalf_of_token",
                statusCode = response.status,
                response = response.error.error,
            )
        }
    }

    override suspend fun clientCredentialsToken(targetScope: String): String {
        return when (val response = texasClient.clientCredentialsToken(targetScope)) {
            is TokenResponse.Success -> response.accessToken
            is TokenResponse.Error -> throw UnexpectedResponseException(
                message = "Kunne ikke autentisere mot $targetScope feilmelding: ${response.error.errorDescription}",
                endpoint = "texas_client_credentials_token",
                statusCode = response.status,
                response = response.error.error,
            )
        }
    }
}
