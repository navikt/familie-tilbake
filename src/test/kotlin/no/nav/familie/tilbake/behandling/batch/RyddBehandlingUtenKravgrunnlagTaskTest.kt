package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.fagsystem
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.UUID

internal class RyddBehandlingUtenKravgrunnlagTaskTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var ryddBehandlingUtenKravgrunnlagTask: RyddBehandlingUtenKravgrunnlagTask

    @Autowired
    private lateinit var taskService: TaskService

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { ContextService.hentSaksbehandler(any()) }.returns("Z0000")
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }.returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.SYSTEM_TILGANG to Behandlerrolle.SYSTEM)))
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `skal hennlegge behandling uten kravgrunnlag som har sendt brev`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.lagBehandling().copy(status = Behandlingsstatus.UTREDES))
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))

        shouldNotThrow<RuntimeException> { ryddBehandlingUtenKravgrunnlagTask.doTask(lagTask(behandling.id)) }

        taskService
            .findAll()
            .any {
                it.type == LagOppgaveTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeTrue()
        val opprettetTask = taskService.finnTaskMedPayloadOgType(behandling.id.toString(), LagOppgaveTask.TYPE)

        val beskrivelse =
            "Tilbakekrevingsbehandlingen for stønad ${opprettetTask?.fagsystem()} opprettet ${behandling.opprettetDato} ble opprettet for over 8 uker siden og har ikke mottatt kravgrunnlag. " +
                "Med mindre det er foretatt en revurdering med tilbakekrevingsbeløp i dag eller de siste dagene for stønaden, så vil det ikke oppstå et kravgrunnlag i dette tilfellet. Tilbakekrevingsbehandlingen kan derfor henlegges manuelt."

        println("${opprettetTask?.metadata?.getProperty("beskrivelse")}")

        Assertions.assertEquals(beskrivelse, opprettetTask?.metadata?.getProperty("beskrivelse"))
        Assertions.assertEquals(behandlingService.hentBehandling(behandling.id).status, Behandlingsstatus.UTREDES)
    }

    @Test
    fun `skal hennlegge behandling uten kravgrunnlag som ikke har sendt brev`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.lagBehandling().copy(status = Behandlingsstatus.UTREDES))
        // brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))

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
