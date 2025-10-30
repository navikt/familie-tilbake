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
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClientImpl
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponse
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test
import java.util.UUID

class DokdistClientImplTest {
    private val logContext = SecureLog.Context.tom()
    private val jwtToken = mockk<JwtToken>()
    private val scope = "api://dokdist/.default"

    private val tokenExchange = mockk<TokenExchangeService> {
        coEvery { clientCredentialsToken(scope) } returns "BEARER"
    }

    @Test
    fun `sendBrev returnerer body ved success`() {
        val forventetResponse = DistribuerJournalpostResponse(
            bestillingsId = "123456789",
        )

        every { jwtToken.encodedToken } returns "USER_TOKEN"

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/rest/v1/distribuerjournalpost"
            req.headers[HttpHeaders.Authorization] shouldBe "Bearer BEARER"

            respond(
                content = jacksonObjectMapper().writeValueAsString(forventetResponse),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }

        val dokdistService = DokdistClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )

        val request = DistribuerJournalpostRequest(
            journalpostId = "",
            batchId = null,
            bestillendeFagsystem = "",
            adresse = null,
            dokumentProdApp = "",
            distribusjonstype = Distribusjonstype.VIKTIG,
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
        )

        val resp = runBlocking {
            dokdistService.sendBrev(
                request = request,
                behandlingId = UUID.randomUUID(),
                logContext = logContext,
            )
        }

        resp shouldBe forventetResponse
        coVerify { tokenExchange.clientCredentialsToken(scope) }
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

        val dokdistService = DokdistClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )

        val request = DistribuerJournalpostRequest(
            journalpostId = "",
            batchId = null,
            bestillendeFagsystem = "",
            adresse = null,
            dokumentProdApp = "",
            distribusjonstype = Distribusjonstype.VIKTIG,
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
        )

        shouldThrow<Exception> {
            runBlocking {
                dokdistService.sendBrev(
                    request = request,
                    behandlingId = UUID.randomUUID(),
                    logContext = logContext,
                )
            }
        }.message shouldContain "Utsending av brev feilet: Utsendig av brev for behandling:"
    }
}
