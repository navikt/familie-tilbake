package no.tilbakekreving.integrasjoner.norg2

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.coroutines.runBlocking
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.norg2.kontrakter.NavKontorEnhet

class Norg2ClientImpl(
    private val config: Norg2Client.Companion.Config,
    private val httpClient: HttpClient,
) : Norg2Client {
    override fun hentNavkontor(enhetId: String): NavKontorEnhet {
        return runBlocking {
            hentNavkontorFraNorg2(enhetId)
        }
    }

    private suspend fun hentNavkontorFraNorg2(enhetId: String): NavKontorEnhet {
        val baseUrl = config.baseUrl
        val scope = config.scope

        val response = httpClient.get {
            url {
                takeFrom(baseUrl)
                path("norg2", "api", "v1", "enhet", enhetId)
            }
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                return response.body<NavKontorEnhet>()
            }
            else -> throw UnexpectedResponseException(
                message = "Henting av navkontorEnhet for enhetId: $enhetId feilet.",
                statusCode = response.status,
                response = response.bodyAsText(),
            )
        }
    }
}
