package no.tilbakekreving.integrasjoner.azure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import kotlinx.coroutines.runBlocking
import no.tilbakekreving.integrasjoner.azure.kontrakter.AzureAdBruker
import no.tilbakekreving.integrasjoner.azure.kontrakter.AzureAdBrukere
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

class AzureGraphClientImpl(
    private val config: AzureGraphClient.Companion.Config,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient,
) : AzureGraphClient {
    override fun finnSaksbehandler(navIdent: String): AzureAdBrukere {
        return runBlocking {
            finnSaksbehandlerIAzure(navIdent)
        }
    }

    override fun hentSaksbehandler(id: String): AzureAdBruker {
        return runBlocking {
            hentSaksbehandlerFraAzure(id)
        }
    }

    private suspend fun finnSaksbehandlerIAzure(navIdent: String): AzureAdBrukere {
        val baseUrl = config.baseUrl
        val scope = config.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                encodedPath = "$encodedPath/$USERS"
                parameters.append("\$search", "\"onPremisesSamAccountName:$navIdent\"")
                parameters.append("\$select", FELTER)
            }
            headers {
                append("Authorization", "Bearer $token")
                append("ConsistencyLevel", "eventual")
            }
        }
        when (response.status) {
            HttpStatusCode.OK -> return response.body<AzureAdBrukere>()
            else -> throw UnexpectedResponseException(
                message = "Feil ved henting av saksbehandler med nav ident: $navIdent",
                statusCode = response.status,
                response = response.body(),
            )
        }
    }

    private suspend fun hentSaksbehandlerFraAzure(id: String): AzureAdBruker {
        val baseUrl = config.baseUrl
        val scope = config.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                encodedPath = "$encodedPath/$USERS/$id"
                parameters.append("\$select", FELTER)
            }
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        when (response.status) {
            HttpStatusCode.OK -> return response.body<AzureAdBruker>()
            else -> throw UnexpectedResponseException(
                message = "Feil ved henting av saksbehandler med id: $id",
                statusCode = response.status,
                response = response.body(),
            )
        }
    }

    companion object {
        private const val USERS = "users"
        private const val FELTER = "givenName,surname,onPremisesSamAccountName,id,userPrincipalName,streetAddress,city"
    }
}
