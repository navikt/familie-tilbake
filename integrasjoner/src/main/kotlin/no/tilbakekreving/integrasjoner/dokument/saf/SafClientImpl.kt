package no.tilbakekreving.integrasjoner.dokument.saf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.dokument.kontrakter.Bruker
import no.tilbakekreving.integrasjoner.dokument.kontrakter.IntegrasjonTema
import no.tilbakekreving.integrasjoner.dokument.kontrakter.JournalpostResponse
import no.tilbakekreving.integrasjoner.dokument.kontrakter.JournalposterForBrukerRequest
import no.tilbakekreving.integrasjoner.dokument.kontrakter.SafHentJournalpostResponse
import no.tilbakekreving.integrasjoner.dokument.kontrakter.SafJournalpostRequest
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.util.UUID

class SafClientImpl(
    private val config: SafClient.Companion.Config,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient,
) : SafClient {
    override fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
        callContext: CallContext.Saksbehandler,
    ): ByteArray {
        return runBlocking {
            safHentDokument(behandlingId, journalpostId, dokumentInfoId, callContext)
        }
    }

    private suspend fun safHentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
        callContext: CallContext.Saksbehandler,
    ): ByteArray {
        val baseUrl = config.baseUrl
        val scope = config.scope

        val token = callContext.exchange(tokenExchangeService, scope)

        val response = httpClient.get("$baseUrl/rest/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                return response.body<ByteArray>()
            }
            else -> throw UnexpectedResponseException(
                message = "Henting av dokument for behandling: $behandlingId feilet.",
                statusCode = response.status,
                response = response.bodyAsText(),
            )
        }
    }

    override fun hentJournalposterForBruker(
        bruker: Bruker,
        tema: List<IntegrasjonTema>,
        graphqlQuery: String,
    ): List<JournalpostResponse> {
        return runBlocking {
            hentJournalposter(bruker, tema, graphqlQuery)
        }
    }

    private suspend fun hentJournalposter(
        bruker: Bruker,
        tema: List<IntegrasjonTema>,
        graphqlQuery: String,
    ): List<JournalpostResponse> {
        val journalposterForBrukerRequest = JournalposterForBrukerRequest(
            antall = 1000,
            brukerId = bruker,
            tema = tema,
        )

        val safJournalpostRequest =
            SafJournalpostRequest(
                journalposterForBrukerRequest,
                graphqlQuery,
            )

        val baseUrl = config.baseUrl
        val scope = config.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        val response = httpClient.post("$baseUrl/graphql") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(safJournalpostRequest)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val safResponse: SafHentJournalpostResponse = response.body()
                return safResponse.data?.dokumentoversiktBruker?.journalposter ?: emptyList()
            }
            else -> throw UnexpectedResponseException(
                message = "Henting av journalposter feilet.",
                statusCode = response.status,
                response = response.bodyAsText(),
            )
        }
    }
}
