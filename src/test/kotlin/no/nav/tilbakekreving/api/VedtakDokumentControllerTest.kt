package no.nav.tilbakekreving.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigInteger
import java.util.UUID

class VedtakDokumentControllerTest {
    private val vedtakDokumentService = mockk<VedtakDokumentService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val tilgangskontrollService = mockk<TilgangskontrollService>()
    private val controller = VedtakDokumentController(vedtakDokumentService, tilbakekrevingService, tilgangskontrollService)
    private val vedtakId = BigInteger.ONE
    private val tilbakekrevingId = "123"
    private val handling = "Henter dokumentreferanser for iverksatt vedtak"

    @BeforeEach
    fun setUp() {
        clearMocks(vedtakDokumentService, tilbakekrevingService, tilgangskontrollService)
    }

    @Test
    fun `ny modell med sendte dokumenter og duplikater`() {
        val historikk = BrevHistorikk(mutableListOf())
        historikk.lagre(Vedtaksbrev.opprett(SystemKlokke))
        listOf(
            VedtaksdokumentDto("journalpost-1", "dokument-1"),
            VedtaksdokumentDto("journalpost-1", "dokument-1"),
            VedtaksdokumentDto("journalpost-1", "dokument-2"),
            VedtaksdokumentDto("journalpost-2", "dokument-1"),
        ).forEach { dokument ->
            historikk.lagre(
                Vedtaksbrev.opprett(SystemKlokke).apply {
                    brevSendt(dokument.journalpostId, dokument.dokumentInfoId)
                },
            )
        }
        nyModell(historikk)

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(
            VedtaksdokumentDto("journalpost-1", "dokument-1"),
            VedtaksdokumentDto("journalpost-1", "dokument-2"),
            VedtaksdokumentDto("journalpost-2", "dokument-1"),
        )
        verifiserNyModell()
    }

    @Test
    fun `ny modell med tom brevhistorikk`() {
        nyModell(BrevHistorikk(mutableListOf()))

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe emptyList()
        verifiserNyModell()
    }

    @Test
    fun `ny modell med tilbakekrevingId men uten tilbakekreving`() {
        every { vedtakDokumentService.hentTilbakekrevingIdNyModell(vedtakId) } returns tilbakekrevingId
        every { tilbakekrevingService.lesTilbakekreving(any(), ValideringContext.ListJournalposter, true) } returns null

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe emptyList()
        verifiserNyModell()
    }

    @Test
    fun `ny modell med avvist tilgang`() {
        val feil = tilgangsfeil()
        every { vedtakDokumentService.hentTilbakekrevingIdNyModell(vedtakId) } returns tilbakekrevingId
        every { tilbakekrevingService.lesTilbakekreving(any(), ValideringContext.ListJournalposter, true) } throws feil

        shouldThrow<Feil> {
            controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())
        } shouldBe feil
        verifiserNyModell()
    }

    @Test
    fun `gammel modell med dokumenter fra flere behandlinger og duplikater`() {
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        val fellesDokument = VedtaksdokumentDto("felles-journalpost", "felles-dokument")
        val førsteDokument = VedtaksdokumentDto("første-journalpost", "første-dokument")
        val andreDokument = VedtaksdokumentDto("andre-journalpost", "andre-dokument")
        gammelModell(listOf(første, andre))
        every { vedtakDokumentService.hentDokumentreferanserGammelModell(første) } returns listOf(fellesDokument, førsteDokument, førsteDokument)
        every { vedtakDokumentService.hentDokumentreferanserGammelModell(andre) } returns listOf(fellesDokument, andreDokument)

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(fellesDokument, førsteDokument, andreDokument)
        verifiserTilgangFørLesing(første)
        verifiserTilgangFørLesing(andre)
        verify { tilbakekrevingService wasNot Called }
    }

    @Test
    fun `gammel modell med behandlinger uten brev`() {
        val behandlingIder = listOf(UUID.randomUUID(), UUID.randomUUID())
        gammelModell(behandlingIder)
        behandlingIder.forEach {
            every { vedtakDokumentService.hentDokumentreferanserGammelModell(it) } returns emptyList()
        }

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe emptyList()
        behandlingIder.forEach { verifiserTilgangFørLesing(it) }
        verify { tilbakekrevingService wasNot Called }
    }

    @Test
    fun `gammel modell uten behandlinger`() {
        gammelModell(emptyList())

        val response = controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe emptyList()
        verify { listOf(tilgangskontrollService, tilbakekrevingService) wasNot Called }
        verify(exactly = 0) { vedtakDokumentService.hentDokumentreferanserGammelModell(any()) }
    }

    @Test
    fun `gammel modell med avvist tilgang til første behandling`() {
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        val feil = tilgangsfeil()
        gammelModell(listOf(første, andre))
        every {
            tilgangskontrollService.validerTilgangBehandlingID(første, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
        } throws feil

        shouldThrow<Feil> {
            controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())
        } shouldBe feil
        verify(exactly = 1) {
            tilgangskontrollService.validerTilgangBehandlingID(første, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
        }
        verify(exactly = 0) {
            vedtakDokumentService.hentDokumentreferanserGammelModell(any())
            tilgangskontrollService.validerTilgangBehandlingID(andre, any(), any(), any())
        }
        verify { tilbakekrevingService wasNot Called }
    }

    @Test
    fun `gammel modell med avvist tilgang etter en tillatt behandling`() {
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        val feil = tilgangsfeil()
        gammelModell(listOf(første, andre))
        every { vedtakDokumentService.hentDokumentreferanserGammelModell(første) } returns listOf(VedtaksdokumentDto("journalpost", "dokument"))
        every {
            tilgangskontrollService.validerTilgangBehandlingID(andre, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
        } throws feil

        shouldThrow<Feil> {
            controller.dokumenterHentVedtaksdokumenter(vedtakId.toString())
        } shouldBe feil
        verifiserTilgangFørLesing(første)
        verifyOrder {
            vedtakDokumentService.hentDokumentreferanserGammelModell(første)
            tilgangskontrollService.validerTilgangBehandlingID(andre, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
        }
        verify(exactly = 1) {
            tilgangskontrollService.validerTilgangBehandlingID(andre, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
        }
        verify(exactly = 0) { vedtakDokumentService.hentDokumentreferanserGammelModell(andre) }
        verify { tilbakekrevingService wasNot Called }
    }

    private fun nyModell(historikk: BrevHistorikk) {
        val tilbakekreving = mockk<Tilbakekreving>()
        every { tilbakekreving.brevHistorikk } returns historikk
        every { vedtakDokumentService.hentTilbakekrevingIdNyModell(vedtakId) } returns tilbakekrevingId
        every { tilbakekrevingService.lesTilbakekreving(any(), ValideringContext.ListJournalposter, true) } returns tilbakekreving
    }

    private fun verifiserNyModell() {
        val filter = slot<TilbakekrevingFilter>()
        verify(exactly = 1) {
            vedtakDokumentService.hentTilbakekrevingIdNyModell(vedtakId)
            tilbakekrevingService.lesTilbakekreving(capture(filter), ValideringContext.ListJournalposter, true)
        }
        // Filtertypen er privat og har ikke verdi-likhet eller en offentlig ID.
        filter.captured.javaClass shouldBe TilbakekrevingFilter.tilbakekreving(tilbakekrevingId).javaClass
        filter.captured.javaClass.getDeclaredField("id").apply { isAccessible = true }.get(filter.captured) shouldBe tilbakekrevingId
        verify(exactly = 0) {
            vedtakDokumentService.hentBehandlingIderGammelModell(any())
            vedtakDokumentService.hentDokumentreferanserGammelModell(any())
        }
        verify { tilgangskontrollService wasNot Called }
    }

    private fun gammelModell(behandlingIder: List<UUID>) {
        every { vedtakDokumentService.hentTilbakekrevingIdNyModell(vedtakId) } returns null
        every { vedtakDokumentService.hentBehandlingIderGammelModell(vedtakId) } returns behandlingIder
        every { tilgangskontrollService.validerTilgangBehandlingID(any(), any(), any(), any()) } just runs
    }

    private fun verifiserTilgangFørLesing(behandlingId: UUID) {
        verify(exactly = 1) {
            tilgangskontrollService.validerTilgangBehandlingID(behandlingId, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
            vedtakDokumentService.hentDokumentreferanserGammelModell(behandlingId)
        }
        verifyOrder {
            tilgangskontrollService.validerTilgangBehandlingID(behandlingId, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, handling)
            vedtakDokumentService.hentDokumentreferanserGammelModell(behandlingId)
        }
    }

    private fun tilgangsfeil(): Feil = Feil(
        message = "Ingen tilgang til dokumenter",
        httpStatus = HttpStatus.FORBIDDEN,
        logContext = SecureLog.Context.tom(),
    )
}
