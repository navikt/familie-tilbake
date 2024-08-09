package no.nav.familie.tilbake.kravgrunnlag

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.data.Testdata.lagBehandling
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class KravvedtakstatusServiceTest {
    private val kravgrunnlagRepository: KravgrunnlagRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val mottattXmlService: ØkonomiXmlMottattService = mockk()
    private val stegService: StegService = mockk()
    private val tellerService: TellerService = mockk()
    private val behandlingskontrollService: BehandlingskontrollService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val historikkTaskService: HistorikkTaskService = mockk()
    private val oppgaveTaskService: OppgaveTaskService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val kravvedtakstatusService = KravvedtakstatusService(
        kravgrunnlagRepository = kravgrunnlagRepository,
        behandlingRepository = behandlingRepository,
        mottattXmlService = mottattXmlService,
        stegService = stegService,
        tellerService = tellerService,
        behandlingskontrollService = behandlingskontrollService,
        behandlingService = behandlingService,
        historikkTaskService = historikkTaskService,
        oppgaveTaskService = oppgaveTaskService,
        oppgaveService = oppgaveService
    )

    val behandling = lagBehandling()
    val kravgrunnlag = mockk<Kravgrunnlag431>(relaxed = true)

    @BeforeAll
    fun beforeAll() {
        every { kravgrunnlagRepository.update(any()) } returns kravgrunnlag
        every { behandlingskontrollService.tilbakehoppBehandlingssteg(any(), any()) } just runs
        every { historikkTaskService.lagHistorikkTask(any(), any(), any(), any(), any(), any(), any()) } just runs
        every { oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(any()) } returns Oppgave(oppgavetype = Oppgavetype.GodkjenneVedtak.name)
        every { oppgaveTaskService.ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(any(), any(), any()) } just runs
        every { oppgaveTaskService.oppdaterOppgaveTask(any(), any(), any(), any()) } just runs
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks(answers = false)
    }

    @ParameterizedTest
    @EnumSource(value = Behandlingssteg::class, mode = EnumSource.Mode.EXCLUDE, names = ["VARSEL"])
    fun `håndterSperMeldingMedBehandling - skal opprette tasken FerdigstillEksisterendeOppgaverOgOpprettNyTask dersom nåværende oppgave ikke er BehandleSak, så lenge behandlingen ikke står på behandlingsteg VARSEL`(behandlingssteg: Behandlingssteg) {
        // Arrange
        every { behandlingskontrollService.finnAktivtSteg(any()) } returns behandlingssteg

        // Act
        kravvedtakstatusService.håndterSperMeldingMedBehandling(behandlingId = behandling.id, kravgrunnlag431 = kravgrunnlag)

        // Assert
        verify(exactly = 0) { oppgaveTaskService.oppdaterOppgaveTask(any(), any(), any()) }
        verify(exactly = 1) { oppgaveTaskService.ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(any(), any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Behandlingssteg::class, mode = EnumSource.Mode.EXCLUDE, names = ["VARSEL"])
    fun `håndterSperMeldingMedBehandling - skal oppdatere oppgave dersom nåværende oppgave er BehandleSak, så lenge behandlingen ikke står på behandlingsteg VARSEL`(behandlingssteg: Behandlingssteg) {
        // Arrange
        every { behandlingskontrollService.finnAktivtSteg(any()) } returns behandlingssteg
        every { oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(any()) } returns Oppgave(oppgavetype = Oppgavetype.BehandleSak.name)

        // Act
        kravvedtakstatusService.håndterSperMeldingMedBehandling(behandlingId = behandling.id, kravgrunnlag431 = kravgrunnlag)

        // Assert
        verify(exactly = 1) { oppgaveTaskService.oppdaterOppgaveTask(any(), any(), any()) }
        verify(exactly = 0) { oppgaveTaskService.ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(any(), any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Behandlingssteg::class, mode = EnumSource.Mode.INCLUDE, names = ["VARSEL"])
    fun `håndterSperMeldingMedBehandling - skal verken opprette tasken FerdigstillEksisterendeOppgaverOgOpprettNyTask eller oppdatere nåværende oppgave, så lenge behandlingen står på behandlingsteg VARSEL`(behandlingssteg: Behandlingssteg) {
        // Arrange
        every { behandlingskontrollService.finnAktivtSteg(any()) } returns behandlingssteg

        // Act
        kravvedtakstatusService.håndterSperMeldingMedBehandling(behandlingId = behandling.id, kravgrunnlag431 = kravgrunnlag)

        // Assert
        verify(exactly = 0) { oppgaveTaskService.oppdaterOppgaveTask(any(), any(), any()) }
        verify(exactly = 0) { oppgaveTaskService.ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(any(), any(), any()) }
    }
}