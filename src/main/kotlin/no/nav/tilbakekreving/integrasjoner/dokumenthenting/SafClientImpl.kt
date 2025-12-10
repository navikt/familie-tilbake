package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain.SafHentJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain.SafJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.felles.graphqlQuery
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.util.UUID

class SafClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
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
        val userToken = tokenValidationContextHolder.getTokenValidationContext().firstValidToken ?: throw Feil(
            message = "Fant ikke user token for $behandlingId",
            frontendFeilmelding = "Fant ikke user token for $behandlingId",
            logContext = logContext,
        )

        val token = tokenExchangeService.onBehalfOfToken(userToken.encodedToken, scope)
        try {
            val response = httpClient.get("$baseUrl/rest/hentdokument/$journalpostId/$dokumentInfoId/ARKIV") {
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
                    frontendFeilmelding = "Henting av dokument for behandling: $behandlingId feilet. Status: ${response.status}",
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
        return runBlocking {
            hentJournalposterForBruker(tilbakekreving)
        }
    }

    private suspend fun hentJournalposterForBruker(tilbakekreving: Tilbakekreving): List<Journalpost> {
        val fagsak = tilbakekreving.eksternFagsak
        val bruker = tilbakekreving.bruker
        val journalposterForBrukerRequest = JournalposterForBrukerRequest(
            antall = 1000,
            brukerId =
                Bruker(
                    id = bruker!!.hentBrukerinfo().ident,
                    type = BrukerIdType.FNR,
                ),
            tema = listOf(fagsak.hentYtelse().tilTema()),
        )

        val safJournalpostRequest =
            SafJournalpostRequest(
                journalposterForBrukerRequest,
                graphqlQuery("/saf/journalposterForBruker.graphql"),
            )

        val baseUrl = applicationProperties.saf.baseUrl
        val scope = applicationProperties.saf.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        try {
            val response = httpClient.post("$baseUrl/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(safJournalpostRequest)
            }
            if (response.status.isSuccess()) {
                val safResponse: SafHentJournalpostResponse = response.body()
                return safResponse.data?.dokumentoversiktBruker?.journalposter ?: emptyList()
            } else {
                val body = response.bodyAsText()
                throw Feil(
                    message = "Henting av journalposter feilet. Status: ${response.status}, melding: $body ",
                    frontendFeilmelding = "Henting av journalposter feilet. Status: ${response.status}",
                    logContext = SecureLog.Context.tom(),
                )
            }
        } catch (e: Exception) {
            throw Feil(
                message = "Henting av journalposter feilet.: $e",
                frontendFeilmelding = "Henting av journalposter feilet.",
                logContext = SecureLog.Context.tom(),
            )
        }
    }
}
