package no.nav.familie.tilbake.http

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class BearerTokenClientCredentialsClientInterceptor(
    oAuth2AccessTokenService: OAuth2AccessTokenService,
    clientConfigurationProperties: ClientConfigurationProperties,
) : AbstractBearerTokenInterceptor(oAuth2AccessTokenService, clientConfigurationProperties) {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val clientProperties = clientPropertiesForGrantType(findByURI(request.uri), GrantType.CLIENT_CREDENTIALS, request.uri)
        request.headers.setBearerAuth(
            genererAccessToken(clientProperties),
        )
        return execution.execute(request, body)
    }
}
