package no.nav.familie.tilbake.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.familie.log.filter.LogFilter
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@SpringBootConfiguration
@ComponentScan(ApplicationConfig.pakkenavn, "no.nav.familie.sikkerhet")
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger"])
@ConfigurationPropertiesScan
class ApplicationConfig {


    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        val serverFactory = JettyServletWebServerFactory()
        serverFactory.port = 8030
        return serverFactory
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()

    companion object {

        const val pakkenavn = "no.nav.familie.tilbake"
    }
}
