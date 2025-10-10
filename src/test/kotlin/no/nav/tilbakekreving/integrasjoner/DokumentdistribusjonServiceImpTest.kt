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
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokumentdistribusjonServiceImp
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequestTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponseTo
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.pdf.PdfGenerator
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test

class DokumentdistribusjonServiceImpTest {
    private val logContext = SecureLog.Context.tom()
    private val jwtToken = mockk<JwtToken>()
    private val scope = "api://dokdist/.default"
    private val tokenHolder = mockk<TokenValidationContextHolder> {
        every { getTokenValidationContext() } returns tokenValidationContextWith("USER_TOKEN")
    }

    private val tokenExchange = mockk<TokenExchangeService> {
        coEvery { onBehalfOfToken("USER_TOKEN", scope) } returns "BEARER"
    }

    @Test
    fun `sendBrev returnerer body ved success`() {
        val expectedResponse = DistribuerJournalpostResponseTo(
            bestillingsId = "123456789",
        )

        every { jwtToken.encodedToken } returns "USER_TOKEN"

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/rest/v1/distribuerjournalpost"
            req.headers[HttpHeaders.Authorization] shouldBe "Bearer BEARER"

            respond(
                content = jacksonObjectMapper().writeValueAsString(expectedResponse),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }

        val dokdistService = DokumentdistribusjonServiceImp(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            tokenValidationContextHolder = tokenHolder,
            httpClient = httpClient,
        )

        val resp = runBlocking {
            dokdistService.sendBrev(
                journalpostId = "1234",
                fagsystem = FagsystemDTO.TS,
                behandlingId = UUID.randomUUID(),
                fagsystemId = "1111",
                logContext = logContext,
            )
        }

        resp shouldBe expectedResponse
        coVerify { tokenExchange.onBehalfOfToken("USER_TOKEN", scope) }
    }

    @Test
    fun `sendBrev returnerer null ved feil`() {
        every { jwtToken.encodedToken } returns "USER_TOKEN"
        val engine = MockEngine {
            respond(
                content = """{"message":"bad request"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { jackson() } }

        val dokdistService = DokumentdistribusjonServiceImp(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            tokenValidationContextHolder = tokenHolder,
            httpClient = httpClient,
        )

        runBlocking {
            dokdistService.sendBrev(
                journalpostId = "1234",
                fagsystem = FagsystemDTO.TS,
                behandlingId = UUID.randomUUID(),
                fagsystemId = "1111",
                logContext = logContext,
            )
        } shouldBe null

    }
}