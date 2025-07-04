package no.nav.familie.tilbake.oppgave

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.kontrakter.oppgave.OppgaveResponse
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.Properties

internal class OppdaterAnsvarligSaksbehandlerTaskTest {
    private val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    private val fagsakRepository: FagsakRepository = mockk(relaxed = true)
    private val mockOppgaveService: OppgaveService = mockk(relaxed = true)
    private val oppgavePrioritetService = mockk<OppgavePrioritetService>()
    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    private val oppdaterAnsvarligSaksbehandlerTask =
        OppdaterAnsvarligSaksbehandlerTask(mockOppgaveService, behandlingRepository, oppgavePrioritetService)

    @BeforeEach
    fun init() {
        clearMocks(mockOppgaveService)
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        every { fagsakRepository.findById(fagsak.id) } returns Optional.of(fagsak)
        every { behandlingRepository.findById(behandling.id) } returns Optional.of(behandling)
        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
    }

    @Test
    fun `doTask skal oppdatere oppgave når prioritet endret`() {
        val oppgave = Oppgave(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = OppgavePrioritet.NORM)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.HOY
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgave

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify {
            mockOppgaveService.patchOppgave(
                oppgave.copy(
                    tilordnetRessurs = behandling.ansvarligSaksbehandler,
                    prioritet = OppgavePrioritet.HOY,
                ),
            )
        }
    }

    @Test
    fun `Skal ikke oppdatere oppgave når ingenting er endret`() {
        val oppgave = Oppgave(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = OppgavePrioritet.NORM)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgave

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify(exactly = 0) { mockOppgaveService.patchOppgave(any()) }
    }

    @Test
    fun `doTask skal oppdatere oppgave når saksbehandler endret`() {
        val oppgave = Oppgave(tilordnetRessurs = "Saksbehandler", prioritet = OppgavePrioritet.NORM)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgave

        every { mockOppgaveService.patchOppgave(match { it.tilordnetRessurs == behandling.ansvarligSaksbehandler && it.prioritet == OppgavePrioritet.NORM }) } returns OppgaveResponse(oppgave.id ?: 1)

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify(atLeast = 1) {
            mockOppgaveService.patchOppgave(
                oppgave.copy(
                    tilordnetRessurs = behandling.ansvarligSaksbehandler,
                    prioritet = OppgavePrioritet.NORM,
                ),
            )
        }
    }

    @Test
    fun `Skal kalle patchOppgave med oppdatert prioritet når unntak kastes`() {
        val oppgave = Oppgave(tilordnetRessurs = behandling.ansvarligSaksbehandler, prioritet = OppgavePrioritet.NORM)
        val oppgaveSomFeiler = Oppgave(prioritet = OppgavePrioritet.NORM)

        every { oppgavePrioritetService.utledOppgaveprioritet(any(), any()) } returns OppgavePrioritet.NORM
        every { mockOppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandling.id) } returns oppgaveSomFeiler

        every { mockOppgaveService.patchOppgave(match { it.tilordnetRessurs == behandling.ansvarligSaksbehandler && it.prioritet == OppgavePrioritet.NORM }) } throws RuntimeException("Mock exception")
        every { mockOppgaveService.patchOppgave(match { it.prioritet == OppgavePrioritet.NORM && it.tilordnetRessurs == null }) } returns OppgaveResponse(oppgave.id ?: 1)

        oppdaterAnsvarligSaksbehandlerTask.doTask(lagTask())

        verify(exactly = 2) { mockOppgaveService.patchOppgave(any()) }
        verify {
            mockOppgaveService.patchOppgave(
                match {
                    it.prioritet == OppgavePrioritet.NORM
                },
            )
        }
    }

    private fun lagTask(opprettetAv: String? = null): Task =
        Task(
            type = OppdaterAnsvarligSaksbehandlerTask.TYPE,
            payload = behandling.id.toString(),
            properties =
                Properties().apply {
                    setProperty("oppgavetype", Oppgavetype.BehandleSak.name)
                    setProperty(PropertyName.ENHET, "enhet")
                    if (opprettetAv != null) {
                        setProperty("opprettetAv", opprettetAv)
                    }
                },
        )
}
