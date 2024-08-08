package no.nav.familie.tilbake.oppgave

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.data.Testdata.lagBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FerdigstillEksisterendeOppgaverOgOpprettNyTaskTest {
    val behandlingRepository: BehandlingRepository = mockk()
    val fagsakRepository: FagsakRepository = mockk()
    val oppgaveService: OppgaveService = mockk()
    val oppgavePrioritetService: OppgavePrioritetService = mockk()

    val ferdigstillEksisterendeOppgaverOgOpprettNyTask = FerdigstillEksisterendeOppgaverOgOpprettNyTask(
        behandlingRepository = behandlingRepository,
        fagsakRepository = fagsakRepository,
        oppgaveService = oppgaveService,
        oppgavePrioritetService = oppgavePrioritetService
    )

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `doTask - skal ferdigstille alle åpne tasker og opprette ny oppgave dersom den ikke eksisterer`() {
        val behandling = lagBehandling()
        // Arrange
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling
        every { fagsakRepository.findByIdOrThrow(any()) } returns Testdata.fagsak
        every { oppgaveService.finnOppgave(any(), any(), any()) } returns Pair(FinnOppgaveRequest(tema = Tema.BAR), FinnOppgaveResponseDto(1, listOf(Oppgave(oppgavetype = Oppgavetype.GodkjenneVedtak.name))))
        every { oppgaveService.ferdigstillOppgave(any(), any()) } just runs
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any()) } just runs
        every { oppgavePrioritetService.utledOppgaveprioritet(any()) } returns OppgavePrioritet.NORM

        val oppgavetypeFerdigstillSlot = slot<Oppgavetype>()
        val oppgavetypeOpprettSlot = slot<Oppgavetype>()
        val beskrivelseSlot = slot<String>()
        val fristSlot = slot<LocalDate>()
        val frist = LocalDate.now()
        // Act
        ferdigstillEksisterendeOppgaverOgOpprettNyTask.doTask(Task(
            type = FerdigstillEksisterendeOppgaverOgOpprettNyTask.TYPE,
            payload = objectMapper.writeValueAsString(
                FerdigstillEksisterendeOppgaverOgOpprettNyTask.FerdigstillEksisterendeOppgaverOgOpprettNyDto(
                    behandlingId = behandling.id,
                    ønsketÅpenOppgavetype = Oppgavetype.BehandleSak,
                    beskrivelse = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
                    frist = frist
                ))
        ))

        // Assert
        verify (exactly = 1) {oppgaveService.ferdigstillOppgave(any(), capture(oppgavetypeFerdigstillSlot))}
        assertThat(oppgavetypeFerdigstillSlot.captured).isEqualTo(Oppgavetype.GodkjenneVedtak)

        verify(exactly = 1) {oppgaveService.opprettOppgave(
            behandlingId = any(),
            oppgavetype = capture(oppgavetypeOpprettSlot),
            enhet = any(),
            beskrivelse = capture(beskrivelseSlot),
            fristForFerdigstillelse = capture(fristSlot),
            saksbehandler = any(),
            prioritet = any()
        )}
        assertThat(oppgavetypeOpprettSlot.captured).isEqualTo(Oppgavetype.BehandleSak)
        assertThat(beskrivelseSlot.captured).isEqualTo(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse)
        assertThat(fristSlot.captured).isEqualTo(frist)
    }

    @Test
    fun `doTask - skal verken ferdigstille eller opprette noen ny oppgave, men oppdatere eksisterende dersom oppgaven allerde eksisterer`() {
        val behandling = lagBehandling()
        // Arrange
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling
        every { fagsakRepository.findByIdOrThrow(any()) } returns Testdata.fagsak
        every { oppgaveService.finnOppgave(any(), any(), any()) } returns Pair(FinnOppgaveRequest(tema = Tema.BAR), FinnOppgaveResponseDto(1, listOf(Oppgave(oppgavetype = Oppgavetype.BehandleSak.name))))
        every { oppgavePrioritetService.utledOppgaveprioritet(any()) } returns OppgavePrioritet.NORM
        every { oppgaveService.patchOppgave(any()) } returns mockk()

        // Act
        ferdigstillEksisterendeOppgaverOgOpprettNyTask.doTask(Task(
            type = FerdigstillEksisterendeOppgaverOgOpprettNyTask.TYPE,
            payload = objectMapper.writeValueAsString(
                FerdigstillEksisterendeOppgaverOgOpprettNyTask.FerdigstillEksisterendeOppgaverOgOpprettNyDto(
                    behandlingId = behandling.id,
                    ønsketÅpenOppgavetype = Oppgavetype.BehandleSak,
                    beskrivelse = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
                    frist = LocalDate.now()
                ))
        ))

        // Assert
        verify (exactly = 0) {oppgaveService.ferdigstillOppgave(any(), any())}
        verify(exactly = 0) {oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any(), any())}
        verify(exactly = 1) { oppgaveService.patchOppgave(any()) }
    }
}