package no.tilbakekreving.integrasjoner

import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

sealed class CallContext(
    val behandlingId: String?,
    val fagsystemId: String?,
) {
    internal abstract suspend fun exchange(
        tokenExchangeService: TokenExchangeService,
        scope: String,
    ): String

    class System(
        behandlingId: String?,
        fagsystemId: String,
    ) : CallContext(behandlingId, fagsystemId) {
        override suspend fun exchange(
            tokenExchangeService: TokenExchangeService,
            scope: String,
        ): String {
            return tokenExchangeService.clientCredentialsToken(scope)
        }
    }

    class Saksbehandler(
        behandlingId: String?,
        fagsystemId: String?,
        val userToken: String,
    ) : CallContext(behandlingId, fagsystemId) {
        override suspend fun exchange(
            tokenExchangeService: TokenExchangeService,
            scope: String,
        ): String {
            return tokenExchangeService.onBehalfOfToken(userToken, scope)
        }
    }
}
