package no.nav.tilbakekreving.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter.TokenIntrospectionResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

class VedtaksdokumentTilgangTest {
    private val tokenExchangeService = mockk<TokenExchangeService>()
    private val request = MockHttpServletRequest().apply { addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token") }
    private val tilgang = VedtaksdokumentTilgang(request, tokenExchangeService)
    private val maskinSvar = TokenIntrospectionResponse(active = true, idtyp = "app", roles = listOf("tvangsgrunnlag.read"))

    @BeforeEach
    fun setUp() {
        clearMocks(tokenExchangeService)
        request.removeHeader(HttpHeaders.AUTHORIZATION)
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token")
    }

    @Test
    fun `aktiv maskin med dedikert rolle og ugjennomsiktig token`() {
        coEvery { tokenExchangeService.introspectToken("test-token") } returns maskinSvar

        tilgang.validerMaskintilgang()

        coVerify(exactly = 1) { tokenExchangeService.introspectToken("test-token") }
    }

    @Test
    fun `inaktiv maskin med dedikert rolle`() {
        coEvery { tokenExchangeService.introspectToken("test-token") } returns maskinSvar.copy(active = false)

        shouldThrow<Feil> { tilgang.validerMaskintilgang() }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `aktivt token uten påkrevd maskintype eller rolle`() {
        listOf(
            maskinSvar.copy(idtyp = "user"),
            maskinSvar.copy(idtyp = null),
            maskinSvar.copy(roles = listOf("access_as_application")),
            maskinSvar.copy(roles = null),
        ).forEach { svar ->
            coEvery { tokenExchangeService.introspectToken("test-token") } returns svar

            shouldThrow<ForbiddenError> { tilgang.validerMaskintilgang() }
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["Basic test-token", "Bearer "])
    fun `manglende eller feilformatert Authorization-header`(authorization: String?) {
        request.removeHeader(HttpHeaders.AUTHORIZATION)
        authorization?.let { request.addHeader(HttpHeaders.AUTHORIZATION, it) }

        shouldThrow<Feil> { tilgang.validerMaskintilgang() }.httpStatus shouldBe HttpStatus.UNAUTHORIZED
        coVerify(exactly = 0) { tokenExchangeService.introspectToken(any()) }
    }

    @Test
    fun `introspeksjon er utilgjengelig`() {
        val feil = IllegalStateException("Introspeksjon er utilgjengelig")
        coEvery { tokenExchangeService.introspectToken("test-token") } throws feil

        shouldThrow<IllegalStateException> { tilgang.validerMaskintilgang() } shouldBe feil
    }
}
