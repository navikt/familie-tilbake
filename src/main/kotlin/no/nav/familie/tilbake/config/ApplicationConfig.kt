package no.nav.familie.tilbake.config

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.tilbake.log.LogTracingHttpFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@ComponentScan(ApplicationConfig.PAKKE_NAVN, "no.nav.familie.prosessering", "no.nav.tilbakekreving")
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
@EnableScheduling
@ConfigurationPropertiesScan
class ApplicationConfig {
    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        val serverFactory = JettyServletWebServerFactory()
        serverFactory.port = 8030
        return serverFactory
    }

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

    @Bean
    fun prosesseringInfoProvider(
        @Value("\${rolle.prosessering}") prosesseringRolle: String,
    ) = object :
        ProsesseringInfoProvider {
        override fun hentBrukernavn(): String =
            try {
                SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .getClaims("azuread")
                    .getStringClaim("preferred_username")
            } catch (e: Exception) {
                throw e
            }

        override fun harTilgang(): Boolean = grupper().contains(prosesseringRolle)

        private fun grupper(): List<String> =
            try {
                SpringTokenValidationContextHolder()
                    .getTokenValidationContext()
                    .getClaims("azuread")
                    ?.get("groups") as List<String>? ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
    }

    companion object {
        const val PAKKE_NAVN = "no.nav.familie.tilbake"
    }
}
