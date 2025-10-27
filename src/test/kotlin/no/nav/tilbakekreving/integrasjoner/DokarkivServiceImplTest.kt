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
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivServiceImpl
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.JournalpostType
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.pdf.PdfGenerator
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test

class DokarkivServiceImplTest {
    private val logContext = SecureLog.Context.tom()
    private val jwtToken = mockk<JwtToken>()
    private val scope = "api://dokarkiv/.default"

    private val tokenExchange = mockk<TokenExchangeService> {
        coEvery { clientCredentialsToken(scope) } returns "BEARER"
    }

    @Test
    fun `lagJournalpost returnerer body ved 201 Created`() {
        val expectedResponse = OpprettJournalpostResponse(
            journalpostId = "123456789",
            journalpostferdigstilt = true,
            melding = null,
            dokumenter = emptyList(),
        )
        every { jwtToken.encodedToken } returns "USER_TOKEN"

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
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

        val dokarkivService = DokarkivServiceImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            varselbrevUtil = mockk(),
            pdfFactory = { mockk<PdfGenerator>(relaxed = true) },
            httpClient = httpClient,
        )

        val request = OpprettJournalpostRequest(
            journalpostType = JournalpostType.UTGAAENDE,
            avsenderMottaker = null,
            bruker = null,
            tema = null,
            behandlingstema = null,
            tittel = null,
            kanal = null,
            journalfoerendeEnhet = null,
            eksternReferanseId = null,
            sak = null,
            dokumenter = emptyList(),
        )

        val resp = runBlocking {
            dokarkivService.lagJournalpost(
                request = request,
                ferdigstill = true,
                behandlingId = "behandling-1",
                eksternFagsakId = "fagsak-1",
                logContext = logContext,
            )
        }

        resp shouldBe expectedResponse
        coVerify { tokenExchange.clientCredentialsToken(scope) }
    }

    @Test
    fun `lagJournalpost kaster Feil med body ved 400`() {
        every { jwtToken.encodedToken } returns "USER_TOKEN"
        val engine = MockEngine {
            respond(
                content = """{"message":"bad request"}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) { install(ContentNegotiation) { jackson() } }

        val dokarkivService = DokarkivServiceImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            varselbrevUtil = mockk(),
            httpClient = httpClient,
            pdfFactory = { mockk<PdfGenerator>(relaxed = true) },
        )

        shouldThrow<Feil> {
            runBlocking {
                dokarkivService.lagJournalpost(
                    request = OpprettJournalpostRequest(
                        journalpostType = JournalpostType.UTGAAENDE,
                        avsenderMottaker = null,
                        bruker = null,
                        tema = null,
                        behandlingstema = null,
                        tittel = null,
                        kanal = null,
                        journalfoerendeEnhet = null,
                        eksternReferanseId = null,
                        sak = null,
                        dokumenter = emptyList(),
                    ),
                    ferdigstill = true,
                    behandlingId = "b",
                    eksternFagsakId = "f",
                    logContext = logContext,
                )
            }
        }.message shouldContain "bad request"
    }
}
