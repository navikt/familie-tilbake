package no.nav.tilbakekreving.saksbehandler

import no.tilbakekreving.integrasjoner.entraProxy.EntraProxyClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class EntraProxyClientBean {
    @Bean
    @Primary
    @Profile("e2e", "local", "integrasjonstest")
    fun entraProxyClientStub(): EntraProxyClient {
        return EntraProxyClientStub()
    }
}
