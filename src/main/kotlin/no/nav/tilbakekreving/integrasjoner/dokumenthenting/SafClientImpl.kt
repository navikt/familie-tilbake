package no.nav.tilbakekreving.integrasjoner.dokumenthenting

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.config.ApplicationProperties
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
}
