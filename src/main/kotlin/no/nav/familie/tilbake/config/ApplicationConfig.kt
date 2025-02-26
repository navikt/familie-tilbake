package no.nav.familie.tilbake.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.tilbake.log.LogTracingHttpFilter
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@ComponentScan(ApplicationConfig.PAKKE_NAVN, "no.nav.familie.prosessering", "no.nav.familie.unleash")
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
@EnableScheduling
@EnableCaching
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
        filterRegistration.filter = LogTracingHttpFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()

    /**
     * Overskriver felles sin som bruker proxy, som ikke skal brukes på gcp.
     */
    @Bean
    @Primary
    fun restTemplateBuilder(objectMapper: ObjectMapper): RestTemplateBuilder {
        val jackson2HttpMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestTemplateBuilder()
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .readTimeout(Duration.of(30, ChronoUnit.SECONDS))
            .additionalMessageConverters(listOf(jackson2HttpMessageConverter) + RestTemplate().messageConverters)
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
