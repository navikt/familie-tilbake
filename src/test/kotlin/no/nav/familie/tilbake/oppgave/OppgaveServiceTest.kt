package no.nav.familie.tilbake.oppgave

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.data.Testdata.fagsak
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.person.PersonService
import no.nav.familie.tilbake.totrinn.TotrinnService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.env.Environment
import java.time.LocalDate
import java.util.Properties

class OppgaveServiceTest {
    private val behandlingRepository: BehandlingRepository = mockk(relaxed = true)
    private val fagsakRepository: FagsakRepository = mockk(relaxed = true)
    private val integrasjonerClient: IntegrasjonerClient = mockk(relaxed = true)
    private val personService: PersonService = mockk(relaxed = true)
    private val environment: Environment = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)
    private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository = mockk(relaxed = true)
    private val totrinnService: TotrinnService = mockk(relaxed = true)

    private val mappeIdGodkjenneVedtak = 100
    private val mappeIdBehandleSak = 200
    private val finnMappeResponseDto =
        listOf(
            MappeDto(300, "EF Sak - 50 Behandle sak", enhetsnr = "4489"),
            MappeDto(mappeIdBehandleSak, "50 Tilbakekreving - Klar til behandling", enhetsnr = "4489"),
            MappeDto(mappeIdGodkjenneVedtak, "70 Godkjennevedtak", enhetsnr = "4489"),
            MappeDto(400, "EF Sak - 70 Godkjenne vedtak", enhetsnr = "4489"),
        )

    private lateinit var oppgaveService: OppgaveService
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setUp() {
        clearMocks(integrasjonerClient)
        behandling = Testdata.lagBehandling()
        oppgaveService =
            OppgaveService(
                behandlingRepository,
                fagsakRepository,
                integrasjonerClient,
                personService,
                taskService,
                environment,
            )
        every { fagsakRepository.findByIdOrThrow(fagsak.id) } returns fagsak
        every { behandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns emptyList()
    }

    @Nested
    inner class OpprettOppgave {
        @Test
        fun `skal legge godkjenneVedtak i EF-Sak-70-mappe for enhet 4489`() {
            val slot = CapturingSlot<OpprettOppgaveRequest>()
            every { integrasjonerClient.finnMapper(any()) } returns finnMappeResponseDto

            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.GodkjenneVedtak,
                "4489",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )

            verify { integrasjonerClient.opprettOppgave(capture(slot)) }
            slot.captured.mappeId shouldBe mappeIdGodkjenneVedtak
        }

        @Test
        fun `skal ikke legge oppgave for enhet 4483 i mappe`() {
            val slot = CapturingSlot<OpprettOppgaveRequest>()

            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.GodkjenneVedtak,
                "4483",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )

            verify { integrasjonerClient.opprettOppgave(capture(slot)) }
            slot.captured.mappeId shouldBe null
        }

        @Test
        fun `skal legge behandleSak i EF-Sak-50-mappe for 4489`() {
            val slot = CapturingSlot<OpprettOppgaveRequest>()
            every { integrasjonerClient.finnMapper("4489") } returns finnMappeResponseDto

            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.BehandleSak,
                "4489",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )
            verify { integrasjonerClient.opprettOppgave(capture(slot)) }

            slot.captured.mappeId shouldBe mappeIdBehandleSak
        }

        @Test
        fun `skal ikke legge behandleSak i EF-Sak-50-mappe for verdi ulik 4489`() {
            val slot = CapturingSlot<OpprettOppgaveRequest>()
            every { integrasjonerClient.finnMapper("4489") } returns finnMappeResponseDto

            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.BehandleSak,
                "1578",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )
            verify { integrasjonerClient.opprettOppgave(capture(slot)) }

            slot.captured.mappeId shouldBe null
        }

        @Test
        fun `skal ikke legge behandleSak i noen mappe når ingen mapper matcher`() {
            val kunMapperSomIkkeKanBrukes =
                listOf(
                    MappeDto(300, "EF Sak - 50 Behandle sak", enhetsnr = "4489"),
                    MappeDto(400, "EF Sak - 70 Godkjenne vedtak", enhetsnr = "4489"),
                )

            val slot = CapturingSlot<OpprettOppgaveRequest>()
            every { integrasjonerClient.finnMapper("4489") } returns kunMapperSomIkkeKanBrukes

            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.BehandleSak,
                "4489",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )
            verify { integrasjonerClient.opprettOppgave(capture(slot)) }

            slot.captured.mappeId shouldBe null
        }

        @Test
        fun `skal fungere også etter rettet skrivefeil i gosys `() {
            val mapperMedOrdelingsfeilRettet =
                listOf(
                    MappeDto(300, "50 Behandle sak", enhetsnr = "4489"),
                    // ligger i gosys som Godkjennevedtak 2022-09-01
                    MappeDto(400, "70 Godkjenne vedtak ", enhetsnr = "4489"),
                )

            val slot = CapturingSlot<OpprettOppgaveRequest>()
            every { integrasjonerClient.finnMapper("4489") } returns mapperMedOrdelingsfeilRettet

            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.GodkjenneVedtak,
                "4489",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )
            verify { integrasjonerClient.opprettOppgave(capture(slot)) }

            slot.captured.mappeId shouldBe 400
        }

        @Test
        fun `skal ikke legge godkjenneVedtak oppgaver i EF-Sak-50-mappe når det allerede finnes en`() {
            every { integrasjonerClient.finnMapper("4489") } returns finnMappeResponseDto
            every { integrasjonerClient.finnOppgaver(any()) } returns FinnOppgaveResponseDto(1L, listOf(Oppgave()))
            oppgaveService.opprettOppgave(
                behandling.id,
                Oppgavetype.GodkjenneVedtak,
                "4483",
                "",
                LocalDate.now().plusDays(5),
                "bob",
                OppgavePrioritet.NORM,
            )

            verify(exactly = 0) { integrasjonerClient.opprettOppgave(any()) }
        }

        @Test
        fun `skal legge godkjenneVedtak oppgaver når det allerede finnes en og har en åpen ferdigstilloppgave task`() {
            val slot = CapturingSlot<OpprettOppgaveRequest>()

            every { integrasjonerClient.finnMapper("4489") } returns finnMappeResponseDto
            every { integrasjonerClient.finnOppgaver(any()) } returns FinnOppgaveResponseDto(1L, listOf(Oppgave()))
            val properties = Properties().apply { setProperty("oppgavetype", Oppgavetype.GodkjenneVedtak.name) }
            every { taskService.finnTasksMedStatus(any(), any(), any()) } returns
                listOf(Task(type = FerdigstillOppgaveTask.TYPE, payload = behandling.id.toString(), properties = properties))

            shouldNotThrow<RuntimeException> {
                oppgaveService.opprettOppgave(
                    behandling.id,
                    Oppgavetype.GodkjenneVedtak,
                    "4489",
                    "",
                    LocalDate.now().plusDays(5),
                    "bob",
                    OppgavePrioritet.NORM,
                )
            }

            verify { integrasjonerClient.opprettOppgave(capture(slot)) }

            slot.captured.mappeId shouldBe mappeIdGodkjenneVedtak
        }
    }

    @Test
    fun `ferdigstillOppgave skal ikke ferdigstille hvis det er mer enn én oppgave`() {
        // Arrange
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling
        every { fagsakRepository.findByIdOrThrow(any()) } returns fagsak
        every { integrasjonerClient.finnOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                2L,
                listOf(
                    Oppgave(oppgavetype = Oppgavetype.GodkjenneVedtak.name),
                    Oppgave(oppgavetype = Oppgavetype.BehandleUnderkjentVedtak.name),
                ),
            )
        // Act & Assert
        assertThrows<Feil> { oppgaveService.ferdigstillOppgave(behandling.id, Oppgavetype.GodkjenneVedtak) }
    }

    @Test
    fun `ferdigstillOppgave skal ferdigstille hvis det ikke er mer enn én oppgave av familie tilbake sine oppgavetyper`() {
        // Arrange
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling
        every { fagsakRepository.findByIdOrThrow(any()) } returns fagsak
        every { integrasjonerClient.finnOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                2L,
                listOf(
                    Oppgave(oppgavetype = Oppgavetype.GodkjenneVedtak.name, id = 1L),
                    Oppgave(oppgavetype = "randomUkjentOppgavetype", id = 2L),
                ),
            )
        // Act
        oppgaveService.ferdigstillOppgave(behandling.id, Oppgavetype.GodkjenneVedtak)

        // Assert
        verify(exactly = 1) { integrasjonerClient.ferdigstillOppgave(any()) }
    }
}
