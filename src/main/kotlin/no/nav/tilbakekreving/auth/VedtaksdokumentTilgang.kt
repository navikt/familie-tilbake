package no.nav.tilbakekreving.auth

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class VedtaksdokumentTilgang(
    private val request: HttpServletRequest,
    private val tokenExchangeService: TokenExchangeService,
) {
    fun validerMaskintilgang() {
        val authorization = request.getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
        val token = authorization.substringAfter(' ', "").trim()
        if (!authorization.substringBefore(' ').equals("Bearer", ignoreCase = true) ||
            token.isBlank() ||
            token.any(Char::isWhitespace)
        ) {
            ugyldigToken()
        }
        val claims = runBlocking {
            tokenExchangeService.introspectToken(token)
        }
        if (!claims.active) {
            ugyldigToken()
        }

        if (claims.idtyp != "app" || claims.roles?.contains("tvangsgrunnlag.read") != true) {
            ingenTilgang()
        }
    }

    private fun ugyldigToken(): Nothing = throw Feil(
        message = "Missing or invalid bearer token",
        frontendFeilmelding = "Du må være innlogget",
        httpStatus = HttpStatus.UNAUTHORIZED,
        logContext = SecureLog.Context.tom(),
    )

    private fun ingenTilgang(): Nothing = throw ForbiddenError(
        message = "Klienten har ikke tilgang til vedtaksdokumenter",
        frontendFeilmelding = "Ingen tilgang til vedtaksdokumenter",
        logContext = SecureLog.Context.tom(),
    )
}
