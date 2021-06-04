package no.nav.familie.tilbake.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Data
import no.nav.familie.tilbake.integration.pdl.internal.IdentInformasjon
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlIdenter
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-pdl")
class PdlClientConfig {

    @Bean
    @Primary
    fun pdlClient(): PdlClient {
        val pdlClient: PdlClient = mockk()

        every { pdlClient.hentPersoninfo(any(), any()) } answers {
            Personinfo(ident = "32132132111",
                       fødselsdato = LocalDate.now().minusYears(20),
                       navn = "testverdi",
                       kjønn = Kjønn.MANN)
        }
        every { pdlClient.hentIdenter(any(), any()) } answers {
            PdlHentIdenterResponse(
                    data = Data(PdlIdenter(identer = listOf(IdentInformasjon("123", "AKTORID")))),
                    errors = listOf()
            )
        }
        return pdlClient
    }
}
