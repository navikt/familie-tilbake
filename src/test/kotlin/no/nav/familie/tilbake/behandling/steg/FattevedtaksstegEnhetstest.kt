package no.nav.familie.tilbake.behandling.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.totrinn.TotrinnService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.VurdertTotrinnDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID

class FattevedtaksstegEnhetstest {
    private val mockBehandlingskontrollService: BehandlingskontrollService = mockk()
    private val mockBehandlingRepository: BehandlingRepository = mockk()
    private val mockTotrinnService: TotrinnService = mockk()
    private val mockOppgaveTaskService: OppgaveTaskService = mockk()
    private val historikkService: HistorikkService = mockk()
    private val mockBehandlingsvedtakService: BehandlingsvedtakService = mockk()
    private val mockManuellBrevmottakerRepository: ManuellBrevmottakerRepository = mockk()

    private val fattevedtakssteg =
        Fattevedtakssteg(
            behandlingskontrollService = mockBehandlingskontrollService,
            behandlingRepository = mockBehandlingRepository,
            totrinnService = mockTotrinnService,
            oppgaveTaskService = mockOppgaveTaskService,
            historikkService = historikkService,
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

        every { mockBehandlingRepository.findById(any()) } returns
            Optional.of(
                Behandling(
                    id = behandlingsId,
                    fagsakId = UUID.randomUUID(),
                    type = Behandlingstype.TILBAKEKREVING,
                    ansvarligSaksbehandler = "A123456",
                    behandlendeEnhet = "1234",
                    behandlendeEnhetsNavn = "NAV Danmark",
                    manueltOpprettet = false,
                    begrunnelseForTilbakekreving = "Yes",
                ),
            )
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
                fattevedtakssteg.utførSteg(behandlingsId, fatteVedtaksstegDto, SecureLog.Context.tom())
            }

        assertThat(exception.message, `is`("Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket"))
    }
}
