package no.nav.familie.tilbake.config

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootConfiguration
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableScheduling
@Profile("!integrasjonstest")
class ApplicationConfig {
    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        val serverFactory = JettyServletWebServerFactory()
        serverFactory.port = 8030
        return serverFactory
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
