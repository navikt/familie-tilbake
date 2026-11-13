package no.nav.tilbakekreving.auth

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Tilgang
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
@Primary
class VedtaksdokumentTilgangStub : VedtaksdokumentTilgang {
    override fun valider(token: String?, tilgang: Tilgang) {
        when (token) {
            GODKJENT_TOKEN -> Unit
            UTEN_ROLLE_TOKEN -> throw ForbiddenError(
                message = tilgang.feilmelding,
                frontendFeilmelding = tilgang.frontendFeilmelding,
                logContext = SecureLog.Context.tom(),
            )
            else -> throw Feil(
                message = "Missing or invalid bearer token",
                frontendFeilmelding = "Du må være innlogget",
                httpStatus = HttpStatus.UNAUTHORIZED,
                logContext = SecureLog.Context.tom(),
            )
        }
    }

    companion object {
        const val GODKJENT_TOKEN = "godkjent-token"
        const val UTEN_ROLLE_TOKEN = "uten-rolle-token"
    }
}
