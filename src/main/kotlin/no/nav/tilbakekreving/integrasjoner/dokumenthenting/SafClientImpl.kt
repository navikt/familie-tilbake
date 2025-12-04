package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.BrukerIdType
import no.nav.familie.tilbake.kontrakter.journalpost.Bruker
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain.SafJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.felles.graphqlQuery
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.util.UUID

class SafClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    },
) : SafClient {
    override fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        return runBlocking {
            safHentDokument(behandlingId, journalpostId, dokumentInfoId)
        }
    }

    private suspend fun safHentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray {
        val logContext = SecureLog.Context.medBehandling(fagsystemId = null, behandlingId = behandlingId.toString())
        val baseUrl = applicationProperties.saf.baseUrl
        val scope = applicationProperties.saf.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)
        try {
            val response = httpClient.get("$baseUrl/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            if (response.status.isSuccess()) {
                return response.body<ByteArray>()
            } else {
                val body = response.bodyAsText()
                throw Feil(
                    message = "Henting av dokument for behandling: $behandlingId feilet med status: ${response.status}: og melding: $body",
                    frontendFeilmelding = "Henting av dokument for behandling: $behandlingId feilet med status: ${response.status}: og melding: $body",
                    logContext = logContext,
                )
            }
        } catch (e: Exception) {
            throw Feil(
                message = "Henting av dokument feilet: $e",
                frontendFeilmelding = "Henting av dokument feilet.",
                logContext = logContext,
            )
        }
    }

    override fun hentJournalposter(tilbakekreving: Tilbakekreving): List<Journalpost> {
        println("=======>>>>> hentJournalposter(tilbakekreving)")
        return runBlocking {
            hentJournalposterForBruker(tilbakekreving)
        }
    }

    private suspend fun hentJournalposterForBruker(tilbakekreving: Tilbakekreving): List<Journalpost> {
        println("=======>>>>> hentJournalposterForBruker(tilbakekreving)")
        val fagsak = tilbakekreving.eksternFagsak
        println("=======>>>>> fagsak: $fagsak")

        val bruker = tilbakekreving.bruker
        println("=======>>>>> bruker: $bruker")

        val journalposterForBrukerRequest = JournalposterForBrukerRequest(
            antall = 1000,
            brukerId =
                Bruker(
                    id = bruker!!.hentBrukerinfo().ident,
                    type = BrukerIdType.FNR,
                ),
            tema = listOf(fagsak.hentYtelse().tilTema()),
        )
        println("=======>>>>> journalposterForBrukerRequest: $journalposterForBrukerRequest")

        val safJournalpostRequest =
            SafJournalpostRequest(
                journalposterForBrukerRequest,
                graphqlQuery("/saf/journalposterForBruker.graphql"),
            )
        println("=======>>>>> safJournalpostRequest: $safJournalpostRequest")

        val baseUrl = applicationProperties.saf.baseUrl
        val scope = applicationProperties.saf.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        try {
            println("=======>>>>> baseUrl: $baseUrl")
            println("=======>>>>> scope: $scope")
            val response = httpClient.post("$baseUrl/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(safJournalpostRequest)
            }
            println("=======>>>>> response: $response")
            if (response.status.isSuccess()) {
                println("=============>>>>> Respose fra saf i journalpost henting: ${response.bodyAsText()}")
                return response.body<List<Journalpost>>()
            } else {
                println("=============>>>>> Respose fra saf i journalpost henting FEILET: ${response.bodyAsText()}")
                throw Feil(
                    message = "Henting av journalposter for behandling: ",
                    frontendFeilmelding = "Henting av journalposter for behandling: ",
                    logContext = SecureLog.Context.tom(),
                )
            }
        } catch (e: Exception) {
            throw Feil(
                message = "Henting av journalposter for bruker feilet: $e",
                frontendFeilmelding = "Henting av journalposter for bruker feilet.",
                logContext = SecureLog.Context.tom(),
            )
        }
    }
}
