package no.nav.familie.tilbake.http

import com.nimbusds.oauth2.sdk.GrantType
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.springframework.http.client.ClientHttpRequestInterceptor
import java.net.URI

abstract class AbstractBearerTokenInterceptor(
    protected val oAuth2AccessTokenService: OAuth2AccessTokenService,
    protected val clientConfigurationProperties: ClientConfigurationProperties,
) : ClientHttpRequestInterceptor {
    protected fun genererAccessToken(clientProperties: ClientProperties): String =
        oAuth2AccessTokenService
            .getAccessToken(clientProperties)
            .access_token ?: throw JwtTokenValidatorException("Kunne ikke hente accesstoken")

    protected fun ClientConfigurationProperties.findByURI(uri: URI) =
        registration
            .values
            .filter { uri.toString().startsWith(it.resourceUrl.toString()) }

    protected fun clientPropertiesForGrantType(
        values: List<ClientProperties>,
        grantType: GrantType,
        uri: URI,
    ): ClientProperties =
        values.firstOrNull { grantType == it.grantType }
            ?: error("could not find oauth2 client config for uri=$uri and grant type=$grantType")
}
