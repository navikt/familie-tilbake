package no.nav.tilbakekreving.auth

import io.jsonwebtoken.Jwts
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import org.junit.jupiter.api.Test

class VedtaksdokumentTilgangTest {
    @Test
    fun `forhåndsgodkjent maskin med applikasjonsrolle`() {
        tilgang(maskinClaims()).validerMaskintilgang()
    }

    @Test
    fun `maskintilgang er deaktivert`() {
        val tilgang = tilgang(maskinClaims(), maskinKlient = "")

        shouldThrow<ForbiddenError> {
            tilgang.validerMaskintilgang()
        }
    }

    @Test
    fun `maskin med manglende roles`() {
        val tilgang = tilgang(maskinClaims() - "roles")

        shouldThrow<ForbiddenError> {
            tilgang.validerMaskintilgang()
        }
    }

    @Test
    fun `token uten maskintype`() {
        val tilgang = tilgang(maskinClaims() - "idtyp")

        shouldThrow<ForbiddenError> {
            tilgang.validerMaskintilgang()
        }
    }

    private fun tilgang(
        claims: Map<String, Any>,
        maskinKlient: String = MASKIN_KLIENT,
    ): VedtaksdokumentTilgang {
        val context = TokenValidationContext(
            mapOf("azuread" to JwtToken(Jwts.builder().issuer("azuread").claims(claims).compact())),
        )
        return VedtaksdokumentTilgang(
            tokenValidationContextHolder = mockk {
                every { getTokenValidationContext() } returns context
            },
            preAuthorizedApps = GODKJENTE_APPER,
            maskinKlient = maskinKlient,
        )
    }

    private fun maskinClaims(): Map<String, Any> = mapOf(
        "idtyp" to "app",
        "azp" to KLIENT_ID,
        "roles" to listOf("annen_rolle", "access_as_application"),
    )

    companion object {
        private const val MASKIN_KLIENT = "dev-gcp:team:maskinklient"
        private const val KLIENT_ID = "godkjent-client-id"
        private const val GODKJENTE_APPER =
            """[{"name":"dev-gcp:team:maskinklient","clientId":"godkjent-client-id"}]"""
    }
}
