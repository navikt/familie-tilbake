package no.nav.familie.tilbake.oppgave

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

class FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakTaskTest {
    val behandlingRepository: BehandlingRepository = mockk()
    val fagsakRepository: FagsakRepository = mockk()
    val oppgaveService: OppgaveService = mockk()
    val oppgavePrioritetService: OppgavePrioritetService = mockk()

    val ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask =
        FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask(
            behandlingRepository = behandlingRepository,
            fagsakRepository = fagsakRepository,
            oppgaveService = oppgaveService,
            oppgavePrioritetService = oppgavePrioritetService,
        )

    @AfterEach
    fun afterEach() {
        clearAllMocks(answers = false)
    }

    @Test
    fun `doTask - skal ferdigstille alle åpne tasker og opprette ny oppgave dersom den ikke eksisterer`() {
        val fagsak = Testdata.fagsak()
        val behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        // Arrange
        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)
        every { fagsakRepository.findById(any()) } returns Optional.of(fagsak)
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns Oppgave(oppgavetype = Oppgavetype.GodkjenneVedtak.name)
        every { oppgaveService.ferdigstillOppgave(any(), any()) } just runs
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just runs
        every { oppgavePrioritetService.utledOppgaveprioritet(any()) } returns OppgavePrioritet.NORM

        val oppgavetypeFerdigstillSlot = slot<Oppgavetype>()
        val oppgavetypeOpprettSlot = slot<Oppgavetype>()
        val beskrivelseSlot = slot<String>()
        val fristSlot = slot<LocalDate>()
        val frist = LocalDate.now()
        // Act
        ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.doTask(
            Task(
                type = FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveDto(
                            behandlingId = behandling.id,
                            beskrivelse = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
                            frist = frist,
                        ),
                    ),
            ),
        )

        // Assert
        verify(exactly = 1) { oppgaveService.ferdigstillOppgave(any(), capture(oppgavetypeFerdigstillSlot)) }
        assertThat(oppgavetypeFerdigstillSlot.captured).isEqualTo(Oppgavetype.GodkjenneVedtak)

        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                behandling = any(),
                oppgavetype = capture(oppgavetypeOpprettSlot),
                enhet = any(),
                beskrivelse = capture(beskrivelseSlot),
                fristForFerdigstillelse = capture(fristSlot),
                saksbehandler = any(),
                prioritet = any(),
                logContext = any(),
                behandlesAvApplikasjon = any(),
                saksId = any(),
            )
        }
        assertThat(oppgavetypeOpprettSlot.captured).isEqualTo(Oppgavetype.BehandleSak)
        assertThat(beskrivelseSlot.captured).isEqualTo(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse)
        assertThat(fristSlot.captured).isEqualTo(frist)
    }

    @Test
    fun `doTask - skal ikke ferdigstille dersom det ikke finnes noen åpen GodkjenneVedtak-oppgaven`() {
        val fagsak = Testdata.fagsak()
        val behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        // Arrange
        every { behandlingRepository.findById(any()) } returns Optional.of(behandling)
        every { fagsakRepository.findById(any()) } returns Optional.of(fagsak)
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns null
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just runs
        every { oppgavePrioritetService.utledOppgaveprioritet(any()) } returns OppgavePrioritet.NORM
        // Act
        ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.doTask(
            Task(
                type = FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveDto(
                            behandlingId = behandling.id,
                            beskrivelse = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
                            frist = LocalDate.now(),
                        ),
                    ),
            ),
        )

        // Assert
        verify(exactly = 0) { oppgaveService.ferdigstillOppgave(any(), any()) }
        verify(exactly = 1) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }
}
