package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.http.BearerTokenClientCredentialsClientInterceptor
import no.nav.familie.tilbake.http.BearerTokenClientInterceptor
import no.nav.familie.tilbake.http.ConsumerIdClientInterceptor
import no.nav.familie.tilbake.http.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestOperations

@Configuration
class HttpClientConfig {
    @Bean("azure")
    fun restTemplateEntraIDBearer(
        restTemplateBuilder: RestTemplateBuilder,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestOperations =
        restTemplateBuilder
            .additionalInterceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()

    @Bean("azureClientCredential")
    fun restTemplateClientCredentialEntraIdBearer(
        restTemplateBuilder: RestTemplateBuilder,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientCredentialsClientInterceptor,
    ): RestOperations =
        restTemplateBuilder
            .additionalInterceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()
}
