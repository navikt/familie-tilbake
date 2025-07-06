package no.tilbakekreving.integrasjoner.tokenexchange

import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenResponse

internal class TokenExchangeServiceImpl(
    private val texasClient: TexasClient,
) : TokenExchangeService {
    override suspend fun onBehalfOfToken(
        userToken: String,
        targetScope: String,
    ): String {
        return when (val response = texasClient.exchangeToken(userToken, targetScope)) {
            is TokenResponse.Success -> response.accessToken
            is TokenResponse.Error -> throw UnexpectedResponseException("Kunne ikke autentisere mot $targetScope feilmelding: ${response.error.errorDescription}", response.status)
        }
    }
}
