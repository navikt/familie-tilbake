package no.nav.tilbakekreving.auth

import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Tilgang
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class VedtaksdokumentTilgangImpl(
    private val tokenExchangeService: TokenExchangeService,
) : VedtaksdokumentTilgang {
    override fun valider(token: String?, tilgang: Tilgang) {
        if (token.isNullOrBlank()) throw ugyldigToken()

        val claims = runBlocking { tokenExchangeService.introspectToken(token) }
        if (!claims.active) throw ugyldigToken()

        if (claims.roles?.contains(tilgang.rolle) != true) {
            throw ForbiddenError(
                message = tilgang.feilmelding,
                frontendFeilmelding = tilgang.frontendFeilmelding,
                logContext = SecureLog.Context.tom(),
            )
        }
    }

    private fun ugyldigToken() = Feil(
        message = "Missing or invalid bearer token",
        frontendFeilmelding = "Du må være innlogget",
        httpStatus = HttpStatus.UNAUTHORIZED,
        logContext = SecureLog.Context.tom(),
    )
}
