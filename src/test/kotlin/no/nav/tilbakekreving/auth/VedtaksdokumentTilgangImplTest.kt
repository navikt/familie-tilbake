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

    private val maskinSvar = TokenIntrospectionResponse(active = true, idtyp = "app", roles = listOf("tvangsgrunnlag.read"))

    @Test
    fun `aktivt token med påkrevd rolle godkjennes`() {
        shouldNotThrowAny { tilgang(maskinSvar).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }
    }

    @Test
    fun `inaktivt token er ugyldig`() {
        shouldThrow<Feil> { tilgang(maskinSvar.copy(active = false)).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `manglende eller tomt token er ugyldig`() {
        shouldThrow<Feil> { tilgang(maskinSvar).valider(null, Tilgang.TVANGSGRUNNLAG_LES) }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
        shouldThrow<Feil> { tilgang(maskinSvar).valider(" ", Tilgang.TVANGSGRUNNLAG_LES) }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `aktivt token uten påkrevd rolle avvises`() {
        shouldThrow<ForbiddenError> { tilgang(maskinSvar.copy(roles = listOf("annen-rolle"))).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }
        shouldThrow<ForbiddenError> { tilgang(maskinSvar.copy(roles = null)).valider("token", Tilgang.TVANGSGRUNNLAG_LES) }
    }
}
