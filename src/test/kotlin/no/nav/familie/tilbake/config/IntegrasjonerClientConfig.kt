package no.nav.familie.tilbake.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.DokumentInfo
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-integrasjoner")
class IntegrasjonerClientConfig {

    @Bean
    @Primary
    fun integrasjonerClient(): IntegrasjonerClient {
        val integrasjonerClient: IntegrasjonerClient = mockk(relaxed = true)

        every { integrasjonerClient.arkiver(any()) } answers {
            ArkiverDokumentResponse("jpId",
                                    false,
                                    listOf(DokumentInfo("id")))
        }
        return integrasjonerClient
    }
}
