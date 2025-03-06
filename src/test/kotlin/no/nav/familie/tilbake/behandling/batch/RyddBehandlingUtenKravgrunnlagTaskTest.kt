package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.kontrakter.oppgave.OppgavePrioritet
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.oppgave.OppgavePrioritetService
import no.nav.familie.tilbake.oppgave.OppgaveService
import no.nav.familie.tilbake.person.PersonService
import no.nav.tilbakekreving.kontrakter.Fagsystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

internal class RyddBehandlingUtenKravgrunnlagTaskTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var brevSporingService: BrevsporingService

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var oppgavePrioritetService: OppgavePrioritetService

    @Autowired
    private lateinit var logService: LogService

    private lateinit var ryddBehandlingUtenKravgrunnlagTask: RyddBehandlingUtenKravgrunnlagTask

    @Autowired
    private lateinit var taskService: TaskService

    private val mockOppgaveService: OppgaveService = mockk(relaxed = true)
    private val personService = mockk<PersonService>()

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { personService.hentAktørId(any(), any(), any()) } returns listOf("123456789")
        ryddBehandlingUtenKravgrunnlagTask =
            RyddBehandlingUtenKravgrunnlagTask(
                behandlingService,
                behandlingRepository,
                brevSporingService,
                logService,
                mockOppgaveService,
                oppgavePrioritetService,
            )
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `skal hennlegge behandling uten kravgrunnlag som har sendt brev`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(status = Behandlingsstatus.UTREDES))
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))
        val fristForFerdigstillelse = LocalDate.now().plusDays(7)
        val prioritet = OppgavePrioritet.NORM
        val beskrivelse =
            "Tilbakekrevingsbehandlingen for stønad ${Fagsystem.BA.name} opprettet ${behandling.opprettetDato} ble opprettet for over 8 uker siden og har ikke mottatt kravgrunnlag. " +
                "Med mindre det er foretatt en revurdering med tilbakekrevingsbeløp i dag eller de siste dagene for stønaden, så vil det ikke oppstå et kravgrunnlag i dette tilfellet. Tilbakekrevingsbehandlingen kan derfor henlegges manuelt."

        ryddBehandlingUtenKravgrunnlagTask.doTask(lagTask(behandling.id))

        verify {
            mockOppgaveService.opprettOppgave(
                any(),
                Oppgavetype.VurderHenvendelse,
                behandling.behandlendeEnhet,
                beskrivelse,
                fristForFerdigstillelse,
                behandling.ansvarligSaksbehandler,
                prioritet,
                any(),
                null,
                null,
            )
        }
    }

    @Test
    fun `skal hennlegge behandling uten kravgrunnlag som ikke har sendt brev`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(status = Behandlingsstatus.UTREDES))

        shouldNotThrow<RuntimeException> { ryddBehandlingUtenKravgrunnlagTask.doTask(lagTask(behandling.id)) }

        taskService
            .findAll()
            .any {
                it.type == LagOppgaveTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeFalse()

        Assertions.assertEquals(behandlingService.hentBehandling(behandling.id).status, Behandlingsstatus.AVSLUTTET)
    }

    private fun lagTask(behandlingId: UUID) =
        Task(
            type = RyddBehandlingUtenKravgrunnlagTask.TYPE,
            payload = behandlingId.toString(),
            Properties().apply {
                setProperty(
                    PropertyName.FAGSYSTEM,
                    Fagsystem.BA.name,
                )
            },
        )
}
