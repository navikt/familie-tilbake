package no.nav.tilbakekreving.pdfGen

import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class PdfGenBean {
    @Bean
    @Primary
    @Profile("e2e", "local", "integrasjonstest")
    fun pdfGenCleintStub(): PdfGenClient {
        return PdfGenClientStub()
    }
}
