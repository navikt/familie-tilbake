package no.nav.familie.tilbake.api.baks

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.tilbake.api.baks.BAKSPorteføljejusteringController.OppdaterBehandlendeEnhetRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.Aktør.Vedtaksløsning
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.navkontor.NavKontorEnhet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class BAKSPorteføljejusteringControllerTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val integrasjonerClient = mockk<IntegrasjonerClient>()
    private val historikkService = mockk<HistorikkService>()

    private val baksPorteføljejusteringController =
        BAKSPorteføljejusteringController(
            behandlingRepository = behandlingRepository,
            integrasjonerClient = integrasjonerClient,
            historikkService = historikkService,
        )

    private val behandlingEksternBrukId = UUID.randomUUID()
    private val nyEnhetId = "1234"

    @Test
    fun `skal kaste feil hvis Behandling ikke finnes for behandlingEksternBrukId`() {
        // Arrange
        val request =
            OppdaterBehandlendeEnhetRequest(
                behandlingEksternBrukId = behandlingEksternBrukId,
                nyEnhet = nyEnhetId,
            )

        every { behandlingRepository.findByEksternBrukId(behandlingEksternBrukId) } returns null

        // Act & Assert
        val exception =
            assertThrows<IllegalStateException> {
                baksPorteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)
            }

        assertThat(exception.message).isEqualTo("Fant ikke behandling for eksternBrukId=$behandlingEksternBrukId")
    }

    @Test
    fun `skal returnere melding om at enhet allerede er satt hvis behandlende enhet er lik ny enhet`() {
        // Arrange
        val behandling = Testdata
            .lagBehandling(fagsakId = UUID.randomUUID())
            .copy(behandlendeEnhet = nyEnhetId)

        val request =
            OppdaterBehandlendeEnhetRequest(
                behandlingEksternBrukId = behandlingEksternBrukId,
                nyEnhet = nyEnhetId,
            )

        every { behandlingRepository.findByEksternBrukId(behandlingEksternBrukId) } returns behandling

        // Act
        val result = baksPorteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)

        // Assert
        assertThat(result.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(result.data).isEqualTo("Behandlende enhet er allerede satt til $nyEnhetId for behandling med eksternBrukId=$behandlingEksternBrukId")

        verify(exactly = 0) { behandlingRepository.update(any()) }
        verify(exactly = 0) { historikkService.lagHistorikkinnslag(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `skal oppdatere behandlende enhet og opprette behandlingshistorikk for BA`() {
        // Arrange
        val behandling = Testdata.lagBehandling(fagsakId = UUID.randomUUID())

        val request =
            OppdaterBehandlendeEnhetRequest(
                behandlingEksternBrukId = behandlingEksternBrukId,
                nyEnhet = nyEnhetId,
            )

        every { behandlingRepository.findByEksternBrukId(behandlingEksternBrukId) } returns behandling
        every { integrasjonerClient.hentNavkontor(nyEnhetId) } returns NavKontorEnhet(
            enhetId = nyEnhetId.toInt(),
            navn = "Nav Kontor",
            enhetNr = "1",
            status = "Eksisterer",
        )
        every { behandlingRepository.update(any()) } returns mockk()
        every { historikkService.lagHistorikkinnslag(any(), any(), any(), any(), any()) } returns mockk()

        // Act
        val result = baksPorteføljejusteringController.oppdaterBehandlendeEnhetPåBehandling(request)

        // Assert
        assertThat(result.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(result.data).isEqualTo("Behandlende enhet oppdatert til $nyEnhetId for behandling med eksternBrukId=$behandling")

        verify(exactly = 1) {
            behandlingRepository.update(
                behandling.copy(
                    ansvarligSaksbehandler = Vedtaksløsning.ident,
                    ansvarligBeslutter = null,
                    behandlendeEnhet = nyEnhetId,
                    behandlendeEnhetsNavn = "Nav Kontor",
                ),
            )
        }

        verify(exactly = 1) {
            historikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = ENDRET_ENHET,
                aktør = Vedtaksløsning,
                opprettetTidspunkt = any(),
                beskrivelse = "Behandlende enhet endret i forbindelse med porteføljejustering.",
            )
        }
    }
}
