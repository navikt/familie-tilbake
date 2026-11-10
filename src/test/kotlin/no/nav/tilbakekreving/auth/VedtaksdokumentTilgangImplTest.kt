package no.nav.tilbakekreving.auth

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.auth.VedtaksdokumentTilgang.Resultat
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenIntrospectionResponse
import org.junit.jupiter.api.Test

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
        tilgang(maskinSvar).vurder("token") shouldBe Resultat.GODKJENT
    }

    @Test
    fun `inaktivt token er ugyldig`() {
        tilgang(maskinSvar.copy(active = false)).vurder("token") shouldBe Resultat.UGYLDIG_TOKEN
    }

    @Test
    fun `manglende eller tomt token er ugyldig`() {
        tilgang(maskinSvar).vurder(null) shouldBe Resultat.UGYLDIG_TOKEN
        tilgang(maskinSvar).vurder(" ") shouldBe Resultat.UGYLDIG_TOKEN
    }

    @Test
    fun `aktivt token uten påkrevd rolle avvises`() {
        tilgang(maskinSvar.copy(roles = listOf("annen-rolle"))).vurder("token") shouldBe Resultat.MANGLER_ROLLE
        tilgang(maskinSvar.copy(roles = null)).vurder("token") shouldBe Resultat.MANGLER_ROLLE
    }
}
