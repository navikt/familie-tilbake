package no.tilbakekreving.integrasjoner.entraProxy

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
import no.tilbakekreving.integrasjoner.entraProxy.kontrakter.Saksbehandler
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

class EntraProxyClientImpl(
    private val config: EntraProxyClient.Companion.Config,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient,
) : EntraProxyClient {
    override fun hentSaksbehandler(id: String): Saksbehandler {
        return runBlocking {
            hentSaksbehandlerFraAzure(id)
        }
    }

    private suspend fun hentSaksbehandlerFraAzure(navIdent: String): Saksbehandler {
        val baseUrl = config.baseUrl
        val scope = config.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        val response = httpClient.get("$baseUrl/api/v1/ansatt/$navIdent") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        when (response.status) {
            HttpStatusCode.OK -> return response.body<Saksbehandler>()
            else -> throw UnexpectedResponseException(
                message = "Feil ved henting av saksbehandler med id: $navIdent",
                statusCode = response.status,
                response = response.bodyAsText(),
            )
        }
    }

    companion object {
        private const val USERS = "users"
        private const val FELTER = "givenName,surname,onPremisesSamAccountName,id,userPrincipalName,streetAddress,city"
    }
}
