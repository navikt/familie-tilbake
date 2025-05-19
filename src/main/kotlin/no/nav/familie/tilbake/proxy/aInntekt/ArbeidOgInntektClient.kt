package no.nav.familie.tilbake.proxy.aInntekt

import com.nimbusds.oauth2.sdk.GrantType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import io.ktor.http.toURI
import no.nav.familie.tilbake.http.BearerTokenClientCredentialsClientInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ArbeidOgInntektClient(
    @Value("ainntekt.baseUrl")
    private val baseUrl: String,
    private val tokenClient: BearerTokenClientCredentialsClientInterceptor,
) {
    private val httpClient = HttpClient(CIO)
    private val redirectUri = URLBuilder()
        .apply {
            takeFrom(baseUrl)
            appendPathSegments("api", "v2", "redirect", "sok", "a-inntekt")
        }.build()

    suspend fun hentAInntektUrl(
        personIdent: String,
    ): String {
        val clientProperties = tokenClient.clientPropertiesForGrantType(tokenClient.findByURI(redirectUri.toURI()), GrantType.CLIENT_CREDENTIALS, redirectUri.toURI())
        val token = tokenClient.genererAccessToken(clientProperties)
        return httpClient
            .get(redirectUri) {
                header("Nav-Personident", personIdent)
                header("Accept", "text/plain")
                header("Authorization", "Bearer $token")
            }.body()
    }
}
