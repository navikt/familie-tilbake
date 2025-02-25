package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.http.BearerTokenClientInterceptor
import no.nav.familie.tilbake.http.ConsumerIdClientInterceptor
import no.nav.familie.tilbake.http.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestOperations

@Configuration
class HttpClientConfig {
    private val restTemplateBuilder = RestTemplateBuilder()

    @Bean("azure")
    fun restTemplateEntraIDBearer(
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
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestOperations =
        restTemplateBuilder
            .additionalInterceptors(
                consumerIdClientInterceptor,
                bearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()
}
