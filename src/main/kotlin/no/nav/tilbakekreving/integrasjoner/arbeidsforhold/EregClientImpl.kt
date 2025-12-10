package no.nav.tilbakekreving.integrasjoner.arbeidsforhold

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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.NotFoundError
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.domain.HentOrganisasjonResponse
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.context.annotation.Profile

@Profile("dev", "prod")
class EregClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    },
) : EregClient {
    override fun hentOrganisasjon(orgnr: String): Organisasjon {
        return runBlocking {
            val organisasjonResponse = hentOrganisasjonNøkkelinfo(orgnr)
            Organisasjon(
                organisasjonsnummer = orgnr,
                navn = organisasjonResponse.navn.sammensattnavn,
            )
        }
    }

    override fun validerOrganisasjon(orgnr: String): Boolean =
        try {
            hentOrganisasjon(orgnr)
            true
        } catch (e: NotFoundError) {
            false
        }

    private suspend fun hentOrganisasjonNøkkelinfo(orgnr: String): HentOrganisasjonResponse {
        val logContext = SecureLog.Context.tom()
        val baseUrl = applicationProperties.eregServices.baseUrl
        val scope = applicationProperties.eregServices.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        val response = httpClient.get("$baseUrl/$orgnr/noekkelinfo") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()

            HttpStatusCode.NotFound -> throw NotFoundError(
                message = "Organisasjon med orgnr $orgnr finnes ikke",
                frontendFeilmelding = "Organisasjon med orgnr $orgnr finnes ikke",
                logContext = logContext,
            )

            else -> {
                val body = response.bodyAsText()
                throw Feil(
                    message = "Henting av organisasjoninfo for orgnr $orgnr feilet med status ${response.status} og melding: $body",
                    frontendFeilmelding = "Henting av organisasjoninfo for orgnr $orgnr feilet med status ${response.status}",
                    logContext = logContext,
                )
            }
        }
    }
}
