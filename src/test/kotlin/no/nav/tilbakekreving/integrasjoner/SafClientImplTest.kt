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
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.kontrakter.journalpost.Journalposttype
import no.nav.familie.tilbake.kontrakter.journalpost.Journalstatus
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.SafClientImpl
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain.DokumentoversiktBruker
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain.SafHentJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain.SafJournalpostData
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test
import java.util.UUID

class SafClientImplTest {
    private val scope = applicationProps().saf.scope
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
    fun `dokumenthenting kaster feil ved ikke-success`() {
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

    @Test
    fun `hentJournalposterForBruker returnerer List av Journalpost ved success`() {
        val safResponse = SafHentJournalpostResponse(
            data = SafJournalpostData(
                dokumentoversiktBruker = DokumentoversiktBruker(
                    journalposter = listOf(
                        Journalpost(
                            journalpostId = "111",
                            journalposttype = Journalposttype.I,
                            journalstatus = Journalstatus.MOTTATT,
                        ),
                    ),
                ),
            ),
        )

        val tilbakekreving = mockk<Tilbakekreving> {
            every { eksternFagsak } returns mockk<EksternFagsak> {
                every { hentYtelse() } returns mockk<Ytelse> {
                    every { tilTema() } returns Tema.TSO
                }
            }
            every { bruker } returns mockk {
                every { hentBrukerinfo() } returns mockk {
                    every { ident } returns "12312312312"
                }
            }
        }

        val engine = MockEngine { req ->
            req.url.fullPath shouldBe "/graphql"
            req.headers[HttpHeaders.Authorization] shouldBe "Bearer BEARER"

            respond(
                content = jacksonObjectMapper().writeValueAsString(safResponse),
                status = HttpStatusCode.OK,
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

        val resp = safClient.hentJournalposter(tilbakekreving)

        resp.size shouldBe 1
        resp[0].journalpostId shouldBe "111"
        coVerify { tokenExchange.clientCredentialsToken(scope) }
    }

    @Test
    fun `hentJournalposterForBruker kaster feil ved ikke-success`() {
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

        val tilbakekreving = mockk<Tilbakekreving> {
            every { eksternFagsak } returns mockk<EksternFagsak> {
                every { hentYtelse() } returns mockk<Ytelse> {
                    every { tilTema() } returns Tema.TSO
                }
            }
            every { bruker } returns mockk {
                every { hentBrukerinfo() } returns mockk {
                    every { ident } returns "12312312312"
                }
            }
        }

        shouldThrow<Exception> {
            safClient.hentJournalposter(tilbakekreving)
        }.message shouldContain "Henting av journalposter feilet. Status:"
    }
}
