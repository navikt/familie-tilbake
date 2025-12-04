package no.nav.tilbakekreving.integrasjoner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
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
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.SafClientImpl
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test
import java.util.UUID

class SafClientImplTest {
    private val scope = "api://saf/.default"
    private val jwtToken = mockk<JwtToken> {
        every { encodedToken } returns "USER_TOKEN"
    }

    private val tokenExchange = mockk<TokenExchangeService> {
        coEvery { clientCredentialsToken(scope) } returns "BEARER"
        coEvery { onBehalfOfToken("USER_TOKEN", scope) } returns "token"
    }

    private val tokenvalidationHolder = mockk<TokenValidationContextHolder> {
        coEvery { getTokenValidationContext() } returns TokenValidationContext(mapOf("" to jwtToken))
    }

    @Test
    fun `dokumenthenting returnerer ByteArray ved success`() {
        val forventetResponse = ByteArray(0)

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/rest/hentdokument/111/222/ARKIV"
            req.headers[HttpHeaders.Authorization] shouldBe "Bearer token"

            respond(
                content = jacksonObjectMapper().writeValueAsString(forventetResponse),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }

        val safClient = SafClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            tokenValidationContextHolder = tokenvalidationHolder,
            httpClient = httpClient,
        )

        val resp = safClient.hentDokument(
            behandlingId = UUID.randomUUID(),
            journalpostId = "111",
            dokumentInfoId = "222",
        )

        resp.shouldBeTypeOf<ByteArray>()
        coVerify { tokenExchange.onBehalfOfToken("USER_TOKEN", scope) }
    }

    @Test
    fun `dokumenthenting kaster exception ved feil`() {
        val engine = MockEngine {
            respond(
                content = """{"message":"bad request"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { jackson() } }

        val safClient = SafClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            tokenValidationContextHolder = tokenvalidationHolder,
            httpClient = httpClient,
        )

        shouldThrow<Exception> {
            safClient.hentDokument(
                behandlingId = UUID.randomUUID(),
                journalpostId = "111",
                dokumentInfoId = "222",
            )
        }.message shouldContain "Henting av dokument for behandling:"
    }
}
