package no.nav.familie.tilbake.http

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI

@Component("entraTokenInterceptor")
class BearerTokenClientInterceptor(
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    clientConfigurationProperties: ClientConfigurationProperties,
) : AbstractBearerTokenInterceptor(oAuth2AccessTokenService, clientConfigurationProperties) {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        request.headers.setBearerAuth(genererAccessToken(clientPropertiesFor(request.uri)))
        return execution.execute(request, body)
    }

    private fun clientPropertiesFor(uri: URI): ClientProperties {
        val clientProperties = findByURI(uri)
        return if (clientProperties.size == 1) {
            clientProperties.first()
        } else {
            clientPropertiesForGrantType(clientProperties, clientCredentialOrJwtBearer(), uri)
        }
    }

    private fun clientCredentialOrJwtBearer() = if (erSystembruker()) GrantType.CLIENT_CREDENTIALS else GrantType.JWT_BEARER

    private fun erSystembruker(): Boolean {
        return try {
            val preferredUsername =
                SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread").get("preferred_username")
            return preferredUsername == null
        } catch (e: Throwable) {
            // Ingen request context. Skjer ved kall som har opphav i kjørende applikasjon. Ping etc.
            true
        }
    }
}
