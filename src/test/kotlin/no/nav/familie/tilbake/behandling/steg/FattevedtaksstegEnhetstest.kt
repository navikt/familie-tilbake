package no.nav.familie.tilbake.behandling.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.api.dto.VurdertTotrinnDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.totrinn.TotrinnService
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class FattevedtaksstegEnhetstest {
    private val mockBehandlingskontrollService: BehandlingskontrollService = mockk()
    private val mockBehandlingRepository: BehandlingRepository = mockk()
    private val mockTotrinnService: TotrinnService = mockk()
    private val mockOppgaveTaskService: OppgaveTaskService = mockk()
    private val mockHistorikkTaskService: HistorikkTaskService = mockk()
    private val mockBehandlingsvedtakService: BehandlingsvedtakService = mockk()
    private val mockManuellBrevmottakerRepository: ManuellBrevmottakerRepository = mockk()

    private val fattevedtakssteg =
        Fattevedtakssteg(
            behandlingskontrollService = mockBehandlingskontrollService,
            behandlingRepository = mockBehandlingRepository,
            totrinnService = mockTotrinnService,
            oppgaveTaskService = mockOppgaveTaskService,
            historikkTaskService = mockHistorikkTaskService,
            behandlingsvedtakService = mockBehandlingsvedtakService,
            manuellBrevmottakerRepository = mockManuellBrevmottakerRepository,
        )

    @Test
    fun `skal kaste feil hvis manuelle brevmottakere ikke er gyldige`() {
        // Arrange
        val behandlingsId = UUID.randomUUID()
        val totrinnsvurderinger: List<VurdertTotrinnDto> =
            listOf(
                VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FAKTA, godkjent = true),
                VurdertTotrinnDto(behandlingssteg = Behandlingssteg.VILKÅRSVURDERING, godkjent = true),
                VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK, godkjent = true),
            )
        val fatteVedtaksstegDto = BehandlingsstegFatteVedtaksstegDto(totrinnsvurderinger)

        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns
            listOf(
                ManuellBrevmottaker(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingsId,
                    navn = "Test testesen",
                    type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    adresselinje1 = "Testadresse med ugyldig postnummer og poststed (fordi det er i utlandet)",
                    postnummer = "0661",
                    poststed = "Oslo",
                ),
            )

        // Act & assert
        val exception =
            assertThrows<Feil> {
                fattevedtakssteg.utførSteg(behandlingsId, fatteVedtaksstegDto)
            }

        assertThat(exception.message, `is`("Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket"))
    }
}
