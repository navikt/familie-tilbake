package no.nav.familie.tilbake.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
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
            PersonInfo(fødselsdato = LocalDate.now().minusYears(20),
                       navn = "testverdi",
                       kjønn = Kjønn.MANN)
        }
        return pdlClient
    }
}
