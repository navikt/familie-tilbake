package no.nav.familie.tilbake.behandling.steg

import io.kotest.matchers.shouldBe
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID

class FattevedtaksstegEnhetstest {
    private val mockBehandlingskontrollService: BehandlingskontrollService = mockk(relaxed = true)
    private val mockBehandlingRepository: BehandlingRepository = mockk()
    private val mockTotrinnService: TotrinnService = mockk(relaxed = true)
    private val mockOppgaveTaskService: OppgaveTaskService = mockk(relaxed = true)
    private val historikkService: HistorikkService = mockk(relaxed = true)
    private val mockBehandlingsvedtakService: BehandlingsvedtakService = mockk(relaxed = true)
    private val mockManuellBrevmottakerRepository: ManuellBrevmottakerRepository = mockk()

    private val behandlingId = UUID.randomUUID()
    private val totrinnsvurderinger: List<VurdertTotrinnDto> = listOf(
        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FAKTA, godkjent = true),
        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.VILKÅRSVURDERING, godkjent = true),
        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK, godkjent = true),
    )
    private val fatteVedtaksstegDto = BehandlingsstegFatteVedtaksstegDto(totrinnsvurderinger)

    private val fattevedtakssteg = Fattevedtakssteg(
        behandlingskontrollService = mockBehandlingskontrollService,
        behandlingRepository = mockBehandlingRepository,
        totrinnService = mockTotrinnService,
        oppgaveTaskService = mockOppgaveTaskService,
        historikkService = historikkService,
        behandlingsvedtakService = mockBehandlingsvedtakService,
        manuellBrevmottakerRepository = mockManuellBrevmottakerRepository,
    )

    @BeforeEach
    fun setup() {
        every { mockBehandlingRepository.findById(any()) } returns Optional.of(
            Behandling(
                id = behandlingId,
                fagsakId = UUID.randomUUID(),
                type = Behandlingstype.TILBAKEKREVING,
                ansvarligSaksbehandler = "A123456",
                ansvarligBeslutter = "B123456",
                behandlendeEnhet = "1234",
                behandlendeEnhetsNavn = "NAV Danmark",
                manueltOpprettet = false,
                begrunnelseForTilbakekreving = "Yes",
            ),
        )
    }

    @Test
    fun `skal kaste feil hvis manuelle brevmottakere ikke er gyldige`() {
        // Arrange
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "Test testesen",
                type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                adresselinje1 = "Testadresse med ugyldig postnummer og poststed (fordi det er i utlandet)",
                postnummer = "0661",
                poststed = "Oslo",
            ),
        )

        val exception = assertThrows<Feil> {
            fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
        }

        exception.message shouldBe "Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket"
    }

    @Test
    fun `sak med organisasjon som fullmektig`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "Advokat Advokatsen",
                orgNr = "889640782",
                type = MottakerType.FULLMEKTIG,
            ),
        )

        fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
    }

    @Test
    fun `sak med organisasjon uten kontaktperson som fullmektig`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "",
                orgNr = "889640782",
                type = MottakerType.FULLMEKTIG,
            ),
        )

        val exception = assertThrows<Feil> {
            fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
        }

        exception.message shouldBe "Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket"
    }

    @Test
    fun `sak med person som fullmektig`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "",
                ident = "20046912345",
                type = MottakerType.FULLMEKTIG,
            ),
        )

        fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
    }

    @Test
    fun `sak med manuell addresse som fullmektig`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "Ola Nordmann",
                landkode = "NO",
                adresselinje1 = "Oppdiktergata 7",
                postnummer = "0456",
                poststed = "Oslo",
                type = MottakerType.FULLMEKTIG,
            ),
        )

        fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
    }

    @Test
    fun `sak med person som verge`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "",
                ident = "20046912345",
                type = MottakerType.VERGE,
            ),
        )

        fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
    }

    @Test
    fun `sak med manuell addresse som verge`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "Ola Nordmann",
                landkode = "NO",
                adresselinje1 = "Oppdiktergata 7",
                postnummer = "0456",
                poststed = "Oslo",
                type = MottakerType.VERGE,
            ),
        )

        fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
    }

    @Test
    fun `sak med manuell addresse som dødsbo`() {
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                navn = "Ola Nordmann",
                landkode = "NO",
                adresselinje1 = "Oppdiktergata 7",
                postnummer = "0456",
                poststed = "Oslo",
                type = MottakerType.DØDSBO,
            ),
        )

        fattevedtakssteg.utførSteg(behandlingId, fatteVedtaksstegDto, SecureLog.Context.tom())
    }
}
