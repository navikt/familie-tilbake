package no.nav.tilbakekreving.arbeidsforhold

import no.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class EregClientStubBean {
    @Bean
    @Primary
    @Profile("e2e", "local", "integrasjonstest")
    fun eregClientStub(): EregClient {
        return EregClientStub()
    }
}
