package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.http.BearerTokenClientCredentialsClientInterceptor
import no.nav.familie.tilbake.http.BearerTokenClientInterceptor
import no.nav.familie.tilbake.http.ConsumerIdClientInterceptor
import no.nav.familie.tilbake.http.MdcValuesPropagatingClientInterceptor
import no.nav.familie.tilbake.log.LogTracingHttpFilter
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestOperations
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule
import java.time.Duration
import java.time.temporal.ChronoUnit

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

    @Bean
    fun logFilter(): FilterRegistrationBean<LogTracingHttpFilter> {
        val filterRegistration = FilterRegistrationBean<LogTracingHttpFilter>()
        filterRegistration.setFilter(LogTracingHttpFilter())
        filterRegistration.order = 1
        return filterRegistration
    }

    @Primary
    @Bean
    fun customizeJackson(): JsonMapperBuilderCustomizer {
        return JsonMapperBuilderCustomizer { builder ->
            builder.addModule(
                kotlinModule {
                    enable(KotlinFeature.KotlinPropertyNameAsImplicitName)
                },
            )
        }
    }

    /**
     * Overskriver felles sin som bruker proxy, som ikke skal brukes på gcp.
     */
    @Bean
    @Primary
    fun restTemplateBuilder(jsonMapper: JsonMapper): RestTemplateBuilder {
        return RestTemplateBuilder()
            .defaultMessageConverters()
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .customizers({ restTemplate ->
                val index = restTemplate.messageConverters.indexOfFirst { it is JacksonJsonHttpMessageConverter }
                restTemplate.messageConverters[index] = JacksonJsonHttpMessageConverter(jsonMapper)
            })
    }
}
