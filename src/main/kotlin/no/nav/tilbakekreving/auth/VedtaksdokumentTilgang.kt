package no.nav.tilbakekreving.auth

import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class VedtaksdokumentTilgang(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    @Value("\${AZURE_APP_PRE_AUTHORIZED_APPS:[]}") preAuthorizedApps: String,
    @Value("\${tilbakekreving.vedtaksdokumenter.maskin-klient:}") private val maskinKlient: String,
) {
    private val forhåndsgodkjenteApper = jacksonObjectMapper().readTree(preAuthorizedApps)

    fun validerMaskintilgang() {
        val claims = tokenValidationContextHolder.getTokenValidationContext().getClaims("azuread")
        if (claims.getStringClaim("idtyp") != "app") {
            ingenTilgang()
        }

        val klientId = claims.getStringClaim("azp")
        val erGodkjentKlient = maskinKlient.isNotBlank() &&
            !klientId.isNullOrBlank() &&
            forhåndsgodkjenteApper.any {
                it.path("name").asText() == maskinKlient && it.path("clientId").asText() == klientId
            }
        if (!erGodkjentKlient || claims.getAsList("roles")?.contains("access_as_application") != true) {
            ingenTilgang()
        }
    }

    private fun ingenTilgang(): Nothing = throw ForbiddenError(
        message = "Klienten har ikke tilgang til vedtaksdokumenter",
        frontendFeilmelding = "Ingen tilgang til vedtaksdokumenter",
        logContext = SecureLog.Context.tom(),
    )
}
