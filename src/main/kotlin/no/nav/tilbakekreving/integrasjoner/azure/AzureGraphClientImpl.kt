package no.nav.tilbakekreving.integrasjoner.azure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.http.headers
import io.ktor.http.takeFrom
import io.ktor.serialization.jackson.jackson
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.integrasjoner.azure.domain.AzureAdBruker
import no.nav.tilbakekreving.integrasjoner.azure.domain.AzureAdBrukere
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import org.springframework.http.HttpStatus

class AzureGraphClientImpl(
    private val applicationProperties: ApplicationProperties,
    private val tokenExchangeService: TokenExchangeService,
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson()
        }
    }
): AzureGraphClient {
    override suspend fun finnSaksbehandler(navIdent: String): AzureAdBrukere {
        val baseUrl = applicationProperties.dokdist.baseUrl
        val scope = applicationProperties.dokdist.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)

        try {
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
            return response.body<AzureAdBrukere>()
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved henting av saksbehandler med nav ident: $navIdent",
                logContext = SecureLog.Context.tom()
            )
        }
    }

    private suspend fun hentSaksbehandler(id: String): AzureAdBruker {
        val baseUrl = applicationProperties.dokdist.baseUrl
        val scope = applicationProperties.dokdist.scope
        val token = tokenExchangeService.clientCredentialsToken(scope)
        try {
            val response = httpClient.get {
                url {
                    takeFrom(baseUrl)
                    encodedPath = "$encodedPath/$USERS/$id"
                    parameters.append("\$select", FELTER)
                }
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            return response.body<AzureAdBruker>()
        } catch (e: Exception) {
            throw Feil(
                message = "Feil ved henting av saksbehandler med id: $id",
                logContext = SecureLog.Context.tom()
            )
        }

    }

    companion object {
        private const val USERS = "users"
        private const val GRUPPER = "memberOf"
        private const val FELTER = "givenName,surname,onPremisesSamAccountName,id,userPrincipalName,streetAddress,city"
        private const val MAX_ANTALL_GRUPPER = 250
    }
}