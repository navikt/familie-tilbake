package no.nav.tilbakekreving.integrasjoner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClientImpl
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.domain.HentOrganisasjonResponse
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.domain.Navn
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test

class EregClientImplTest {
    private val jwtToken = mockk<JwtToken>()
    private val scope = applicationProps().eregServices.scope

    private val tokenExchange = mockk<TokenExchangeService> {
        coEvery { clientCredentialsToken(scope) } returns "BEARER"
    }

    @Test
    fun `henter organisasjon ved success`() {
        val forventetResponse = Organisasjon(organisasjonsnummer = "123456789", navn = "navn")
        val eregClient = hentEregClient()

        val response = eregClient.hentOrganisasjon("123456789")

        response shouldBe forventetResponse
        coVerify { tokenExchange.clientCredentialsToken(scope) }
    }

    @Test
    fun `hentOrganisasjon kaster feil med riktig melding`() {
        every { jwtToken.encodedToken } returns "USER_TOKEN"
        val engine = MockEngine {
            respond(
                content = """{"message":"bad request"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { jackson() } }

        val eregClient = EregClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )

        shouldThrow<Feil> {
            eregClient.hentOrganisasjon("123456789")
        }.message shouldContain "Henting av organisasjoninfo for orgnr"
    }

    @Test
    fun `validering av organisasjon return true ved success`() {
        val eregClient = hentEregClient()
        val response = eregClient.validerOrganisasjon("123456789")
        response shouldBe true
    }

    @Test
    fun `validering av organisasjon return false ved not found`() {
        every { jwtToken.encodedToken } returns "USER_TOKEN"

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/123456789/noekkelinfo"
            req.headers[HttpHeaders.Authorization] shouldBe "Bearer BEARER"

            respond("{}", HttpStatusCode.NotFound)
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }

        val eregClient = EregClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )

        val response = eregClient.validerOrganisasjon("123456789")
        response shouldBe false
    }

    private fun hentEregClient(): EregClient {
        val eregRespons = HentOrganisasjonResponse(
            navn = Navn(sammensattnavn = "navn"),
            adresse = null,
        )

        every { jwtToken.encodedToken } returns "USER_TOKEN"

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/123456789/noekkelinfo"
            req.headers[HttpHeaders.Authorization] shouldBe "Bearer BEARER"

            respond(
                content = jacksonObjectMapper().writeValueAsString(eregRespons),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }

        return EregClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )
    }
}
