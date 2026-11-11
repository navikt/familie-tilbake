package no.nav.tilbakekreving.auth

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.http.HttpStatus

interface VedtaksdokumentTilgang {
    fun vurder(token: String?, tilgang: Tilgang): Resultat

    fun valider(token: String?, tilgang: Tilgang) {
        when (vurder(token, tilgang)) {
            Resultat.GODKJENT -> Unit
            Resultat.UGYLDIG_TOKEN -> throw Feil(
                message = "Missing or invalid bearer token",
                frontendFeilmelding = "Du må være innlogget",
                httpStatus = HttpStatus.UNAUTHORIZED,
                logContext = SecureLog.Context.tom(),
            )
            Resultat.MANGLER_ROLLE -> throw ForbiddenError(
                message = tilgang.feilmelding,
                frontendFeilmelding = tilgang.frontendFeilmelding,
                logContext = SecureLog.Context.tom(),
            )
        }
    }

    enum class Tilgang(
        val rolle: String,
        val feilmelding: String,
        val frontendFeilmelding: String,
    ) {
        TVANGSGRUNNLAG_LES(
            rolle = "tvangsgrunnlag.read",
            feilmelding = "Klienten har ikke tilgang til vedtaksdokumenter",
            frontendFeilmelding = "Ingen tilgang til vedtaksdokumenter",
        ),
    }

    enum class Resultat {
        GODKJENT,
        UGYLDIG_TOKEN,
        MANGLER_ROLLE,
    }
}
