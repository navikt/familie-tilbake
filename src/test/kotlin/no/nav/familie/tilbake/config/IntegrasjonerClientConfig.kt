package no.nav.familie.tilbake.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.DokumentInfo
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.util.UUID

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

        every { integrasjonerClient.distribuerJournalpost(any(), any()) } returns "42"

        every { integrasjonerClient.hentDokument(any(), any()) } returns readMockfileFromResources()

        every { integrasjonerClient.hentOrganisasjon(any()) } returns Organisasjon("987654321", "Bobs Burgers")

        every { integrasjonerClient.hentSaksbehandler(any()) } returns Saksbehandler(UUID.randomUUID(),
                                                                                     "bb1234",
                                                                                     "Bob",
                                                                                     "Burger")
        every { integrasjonerClient.finnOppgaver(any()) } answers {
            FinnOppgaveResponseDto(antallTreffTotalt = 1,
                                   oppgaver = listOf(Oppgave(id = 1)))
        }

        every { integrasjonerClient.ferdigstillOppgave(any()) } just Runs

        every { integrasjonerClient.hentNavkontor(any()) } returns NavKontorEnhet(enhetId = 4806,
                                                                                  navn = "Mock NAV Drammen",
                                                                                  enhetNr = "mock",
                                                                                  status = "mock")

        return integrasjonerClient
    }

    fun readMockfileFromResources(): ByteArray {
        return javaClass.getResource("/mockpdf/mocktest.pdf").readBytes()
    }
}
