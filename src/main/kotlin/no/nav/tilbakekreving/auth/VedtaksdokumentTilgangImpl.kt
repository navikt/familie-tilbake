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
        val claims = token
            ?.takeIf { it.isNotBlank() }
            ?.let { runBlocking { tokenExchangeService.introspectToken(it) } }
        if (claims == null || !claims.active) {
            throw Feil(
                message = "Missing or invalid bearer token",
                frontendFeilmelding = "Du må være innlogget",
                httpStatus = HttpStatus.UNAUTHORIZED,
                logContext = SecureLog.Context.tom(),
            )
        }

        if (claims.roles?.contains(tilgang.rolle) != true) {
            throw ForbiddenError(
                message = tilgang.feilmelding,
                frontendFeilmelding = tilgang.frontendFeilmelding,
                logContext = SecureLog.Context.tom(),
            )
        }
    }
}
