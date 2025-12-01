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
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClientImpl
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.junit.jupiter.api.Test
import java.util.UUID

class DokarkivClientImplTest {
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

        val dokarkivService = DokarkivClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )

        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = "12345678911",
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(
                Dokument(
                    dokument = "dummy data".toByteArray(),
                    filtype = Filtype.PDFA,
                    filnavn = "testfil.pdf",
                    tittel = "Test Tittel",
                ),
            ),
            fagsakId = "1234",
            journalførendeEnhet = "4321",
            eksternReferanseId = "1111",
            avsenderMottaker = AvsenderMottaker(
                id = "12345678911",
                idType = AvsenderMottakerIdType.FNR,
                navn = "Navn Navnesen",
            ),
        )

        val resp = dokarkivService.opprettOgSendJournalpostRequest(
            arkiverDokument = arkiverDokumentRequest,
            fagsaksystem = DokarkivFagsaksystem.TILLEGGSSTONADER,
            brevkode = "brev",
            tema = Tema.TSO,
            dokuemntkategori = DokumentKlasse.B,
            behandlingId = UUID.randomUUID(),
        )

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

        val dokarkivService = DokarkivClientImpl(
            applicationProperties = applicationProps(),
            tokenExchangeService = tokenExchange,
            httpClient = httpClient,
        )

        shouldThrow<Feil> {
            dokarkivService.opprettOgSendJournalpostRequest(
                arkiverDokument = ArkiverDokumentRequest(
                    fnr = "12345678911",
                    forsøkFerdigstill = true,
                    hoveddokumentvarianter = listOf(
                        Dokument(
                            dokument = "dummy data".toByteArray(),
                            filtype = Filtype.PDFA,
                            filnavn = "testfil.pdf",
                            tittel = "Test Tittel",
                        ),
                    ),
                    fagsakId = "1234",
                    journalførendeEnhet = "4321",
                    eksternReferanseId = "1111",
                    avsenderMottaker = AvsenderMottaker(
                        id = "12345678911",
                        idType = AvsenderMottakerIdType.FNR,
                        navn = "Navn Navnesen",
                    ),
                ),
                fagsaksystem = DokarkivFagsaksystem.TILLEGGSSTONADER,
                tema = Tema.TSO,
                dokuemntkategori = DokumentKlasse.B,
                brevkode = "brev",
                behandlingId = UUID.randomUUID(),
            )
        }.message shouldContain "bad request"
    }
}
