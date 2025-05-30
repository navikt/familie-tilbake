package no.nav.familie.tilbake.proxy.aInntekt

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ArbeidOgInntektClient(
    @Value("\${ainntekt.baseUrl}")
    private val baseUrl: String,
) {
    private val httpClient = HttpClient(CIO)
    private val redirectUri = URLBuilder()
        .apply {
            takeFrom(baseUrl)
            appendPathSegments("api", "v2", "redirect", "sok", "a-inntekt")
        }.build()

    suspend fun hentAInntektUrl(
        personIdent: String,
    ): String =
        httpClient
            .get(redirectUri) {
                header("Nav-Personident", personIdent)
                header("Accept", "text/plain")
            }.body()

    @PreDestroy
    fun close() {
        httpClient.close()
    }
}
