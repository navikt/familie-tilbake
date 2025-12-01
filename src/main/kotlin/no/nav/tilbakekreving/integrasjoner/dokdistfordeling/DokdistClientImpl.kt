package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
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
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.AdresseTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequest
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.util.UUID

class DokdistClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    },
) : DokdistClient {
    private suspend fun sendBrev(
        request: DistribuerJournalpostRequest,
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponse {
        val baseUrl = applicationProperties.dokdist.baseUrl
        val scope = applicationProperties.dokdist.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        try {
            val response = httpClient.post("$baseUrl/rest/v1/distribuerjournalpost") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }

            if (response.status.isSuccess()) {
                return response.body<DistribuerJournalpostResponse>()
            } else {
                val body = response.bodyAsText()
                throw Feil(
                    message = "Utsendig av brev for behandling: $behandlingId feilet med status: ${response.status}: og melding: $body",
                    frontendFeilmelding = "Utsendig av brev for behandling: $behandlingId feilet med status: ${response.status}: og melding: $body",
                    logContext = logContext,
                )
            }
        } catch (e: Exception) {
            throw Feil(
                message = "Utsending av brev feilet: ${e.message}",
                frontendFeilmelding = "Utsending av brev feilet.",
                logContext = logContext,
            )
        }
    }

    override fun brevTilUtsending(
        behandlingId: UUID,
        journalpostId: String,
        fagsystem: FagsystemDTO,
        distribusjonstype: Distribusjonstype,
        distribusjonstidspunkt: Distribusjonstidspunkt,
        adresse: AdresseTo?,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponse {
        return runBlocking {
            sendBrev(
                request = opprettDistribuerJournalpostRequest(journalpostId, fagsystem, distribusjonstype, distribusjonstidspunkt, adresse),
                behandlingId = behandlingId,
                logContext = logContext,
            )
        }
    }

    private fun opprettDistribuerJournalpostRequest(
        journalpostId: String,
        fagsystem: FagsystemDTO,
        distribusjonstype: Distribusjonstype,
        distribusjonstidspunkt: Distribusjonstidspunkt,
        addresse: AdresseTo?,
    ): DistribuerJournalpostRequest {
        return DistribuerJournalpostRequest(
            journalpostId = journalpostId,
            batchId = null,
            bestillendeFagsystem = fagsystem.name,
            adresse = addresse,
            dokumentProdApp = "tilbakekreving",
            distribusjonstype = distribusjonstype,
            distribusjonstidspunkt = distribusjonstidspunkt,
        )
    }
}
