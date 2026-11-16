package no.nav.tilbakekreving.auth

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Tilgang
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenIntrospectionResponse
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class VedtaksdokumentTilgangImplTest {
    private fun tilgang(svar: TokenIntrospectionResponse) = VedtaksdokumentTilgangImpl(
        object : TokenExchangeService {
            override suspend fun onBehalfOfToken(userToken: String, targetScope: String): String = error("Ikke i bruk")

            override suspend fun clientCredentialsToken(targetScope: String): String = error("Ikke i bruk")

            override suspend fun introspectToken(token: String): TokenIntrospectionResponse = svar
        },
    )

    private fun introspection(
        active: Boolean = true,
        roles: List<String>? = listOf(Tilgang.TVANGSGRUNNLAG_LES.rolle),
    ) = TokenIntrospectionResponse(active = active, idtyp = "app", roles = roles)

    @Test
    fun `aktivt token med påkrevd rolle`() {
        shouldNotThrowAny { tilgang(introspection()).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }
    }

    @Test
    fun `inaktivt token`() {
        shouldThrow<Feil> { tilgang(introspection(active = false)).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `manglende eller tomt token`() {
        shouldThrow<Feil> { tilgang(introspection()).valider(null, Tilgang.TVANGSGRUNNLAG_LES) }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
        shouldThrow<Feil> { tilgang(introspection()).valider(" ", Tilgang.TVANGSGRUNNLAG_LES) }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `aktivt token uten påkrevd rolle`() {
        shouldThrow<ForbiddenError> { tilgang(introspection(roles = listOf("annen-rolle"))).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }
        shouldThrow<ForbiddenError> { tilgang(introspection(roles = null)).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }
    }
}
