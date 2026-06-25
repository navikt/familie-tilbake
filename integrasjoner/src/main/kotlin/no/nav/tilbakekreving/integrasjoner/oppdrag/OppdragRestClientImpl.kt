package no.nav.tilbakekreving.integrasjoner.oppdrag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService

internal class OppdragRestClientImpl(
    private val config: OppdragRestClient.Companion.Config,
    private val httpClient: HttpClient,
    private val tokenExchangeService: TokenExchangeService,
) : OppdragRestClient {
    override fun iverksettVedtak(request: TilbakekrevingsvedtakRequestDto): TilbakekrevingsvedtakResponseDto {
        return runBlocking {
            val token = tokenExchangeService.clientCredentialsToken(config.scope)
            httpClient.post(
                buildUrl {
                    takeFrom(config.baseUrl)
                    appendPathSegments("/api/v1/tilbakekreving/vedtak")
                },
            ) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
        }
    }
}
