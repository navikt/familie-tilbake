package no.tilbakekreving.integrasjoner.arbeidsforhold

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.HentOrganisasjonResponse
import no.tilbakekreving.integrasjoner.feil.NotFoundException
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

class EregClientImpl(
    private val config: EregClient.Companion.Config,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient,
) : EregClient {
    override fun hentOrganisasjon(orgnr: String): HentOrganisasjonResponse {
        return runBlocking {
            hentOrganisasjonNøkkelinfo(orgnr)
        }
    }

    override fun validerOrganisasjon(orgnr: String): Boolean =
        try {
            hentOrganisasjon(orgnr)
            true
        } catch (e: NotFoundException) {
            false
        }

    private suspend fun hentOrganisasjonNøkkelinfo(orgnr: String): HentOrganisasjonResponse {
        val baseUrl = config.baseUrl
        val scope = config.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        val response = httpClient.get("$baseUrl/$orgnr/noekkelinfo") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.NotFound -> throw NotFoundException(
                statusCode = HttpStatusCode.NotFound,
                message = "Organisasjon med orgnr $orgnr finnes ikke",
                response = response.body(),
            )
            else -> {
                val body = response.bodyAsText()
                throw UnexpectedResponseException(
                    message = "Henting av organisasjoninfo for orgnr $orgnr feilet med status ${response.status} og melding: $body",
                    statusCode = response.status,
                    response = response.body(),
                )
            }
        }
    }
}
