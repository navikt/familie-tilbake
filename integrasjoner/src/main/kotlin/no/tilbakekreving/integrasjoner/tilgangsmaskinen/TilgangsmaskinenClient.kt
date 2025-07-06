package no.tilbakekreving.integrasjoner.tilgangsmaskinen

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import no.tilbakekreving.integrasjoner.tilgangsmaskinen.kontrakter.PersonDetailResponseDTO
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

internal class TilgangsmaskinenClient internal constructor(
    private val config: PersontilgangService.Companion.Config,
    private val client: HttpClient,
    private val tokenExchangeService: TokenExchangeService,
) {
    suspend fun hentPersontilgang(
        callContext: CallContext.Saksbehandler,
        personIdent: String,
    ): PersonDetailResponseDTO? {
        val response = client.post("${config.baseUrl}/api/v1/kjerne") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${tokenExchangeService.onBehalfOfToken(callContext.userToken, config.scope)}")
            setBody(personIdent)
        }

        return when (response.status) {
            HttpStatusCode.NoContent -> return null
            HttpStatusCode.Forbidden -> response.body()
            else -> throw UnexpectedResponseException(
                message = "Uventet svar fra tilgangsmaskinen",
                statusCode = response.status,
            )
        }
    }
}
