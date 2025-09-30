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
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostRequestTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponseTo
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev", "prod")
class DokumentdistribusjonServiceImp(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) : DokumentdistribusjonService {
    private val logger = TracedLogger.getLogger<DokumentdistribusjonServiceImp>()

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }

    override suspend fun sendBrev(
        tilbakekreving: Tilbakekreving,
        req: DistribuerJournalpostRequestTo,
    ): DistribuerJournalpostResponseTo? {
        val logContext = SecureLog.Context.fra(tilbakekreving)

        val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken ?: error("Trenger token!")

        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.internId
        val fagsystemId = tilbakekreving.fagsystemId
        val baseUrl = applicationProperties.dokdist.baseUrl
        val scope = applicationProperties.dokdist.scope

        val callContext = CallContext.Saksbehandler(
            behandlingId.toString(),
            fagsystemId,
            userToken = token.encodedToken,
        )

        val bearerToken = tokenExchangeService.onBehalfOfToken(callContext.userToken, scope)

        try {
            val response = client.post("$baseUrl/rest/v1/distribuerjournalpost") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $bearerToken")
                setBody(req)
            }

            if (response.status.isSuccess()) {
                return response.body<DistribuerJournalpostResponseTo>()
            } else {
                val body = response.bodyAsText()

                logger.medContext(logContext) {
                    error("Utsendig av brev feilet: ${response.status}: $body")
                }
                return null
            }
        } catch (e: Exception) {
            throw Feil(
                message = "Utsending av brev feilet: ${e.message}",
                frontendFeilmelding = "Utsending av brev feilet.",
                logContext = SecureLog.Context.fra(tilbakekreving),
            )
        }
    }
}
