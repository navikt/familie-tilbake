package no.nav.familie.tilbake.oppgave

import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals

internal class LagOppgaveTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    private lateinit var spykOppgaveService: OppgaveService

    private lateinit var lagOppgaveTask: LagOppgaveTask

    private lateinit var behandling: Behandling

    private val behandlingSlot = slot<UUID>()
    private val oppgavetypeSlot = slot<Oppgavetype>()
    private val beskrivelseSlot = slot<String>()
    private val fristForFerdigstillelse = slot<LocalDate>()

    private val dagensDato = LocalDate.now()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        spykOppgaveService = spyk(oppgaveService)
        lagOppgaveTask = LagOppgaveTask(spykOppgaveService, behandlingskontrollService)
    }

    @Test
    fun `doTask skal lage oppgave når behandling venter på varsel steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL, Behandlingsstegstatus.VENTER, Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)

        lagOppgaveTask.doTask(lagTask())

        verify {
            spykOppgaveService.opprettOppgave(capture(behandlingSlot),
                                              capture(oppgavetypeSlot),
                                              capture(beskrivelseSlot),
                                              capture(fristForFerdigstillelse),
                                              isNull())
        }
        assertEquals(behandling.id, behandlingSlot.captured)
        assertEquals(Oppgavetype.BehandleSak, oppgavetypeSlot.captured)
        assertEquals(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.beskrivelse, beskrivelseSlot.captured)
        assertEquals(dagensDato.plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker),
                     fristForFerdigstillelse.captured)
    }

    @Test
    fun `doTask skal lage oppgave når behandling venter på grunnlag steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)

        lagOppgaveTask.doTask(lagTask())

        verify {
            spykOppgaveService.opprettOppgave(capture(behandlingSlot),
                                              capture(oppgavetypeSlot),
                                              capture(beskrivelseSlot),
                                              capture(fristForFerdigstillelse),
                                              isNull())
        }
        assertEquals(behandling.id, behandlingSlot.captured)
        assertEquals(Oppgavetype.BehandleSak, oppgavetypeSlot.captured)
        assertEquals(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse, beskrivelseSlot.captured)
        assertEquals(dagensDato.plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker),
                     fristForFerdigstillelse.captured)
    }

    @Test
    fun `doTask skal lage oppgave når behandling er på FAKTA steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        lagOppgaveTask.doTask(lagTask())

        verify {
            spykOppgaveService.opprettOppgave(capture(behandlingSlot),
                                              capture(oppgavetypeSlot),
                                              isNull(),
                                              capture(fristForFerdigstillelse),
                                              isNull())
        }
        assertEquals(behandling.id, behandlingSlot.captured)
        assertEquals(Oppgavetype.BehandleSak, oppgavetypeSlot.captured)
        assertEquals(dagensDato, fristForFerdigstillelse.captured)
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg,
                                            behandlingsstegsstatus: Behandlingsstegstatus,
                                            venteårsak: Venteårsak? = null) {
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandling.id,
                                                                           behandlingssteg = behandlingssteg,
                                                                           behandlingsstegsstatus = behandlingsstegsstatus,
                                                                           venteårsak = venteårsak,
                                                                           tidsfrist = venteårsak?.let {
                                                                               dagensDato.plusWeeks(it.defaultVenteTidIUker)
                                                                           }))
    }

    private fun lagTask(): Task {
        return Task(type = LagOppgaveTask.TYPE,
                    payload = behandling.id.toString(),
                    properties = Properties().apply { setProperty("oppgavetype", Oppgavetype.BehandleSak.name) })
    }


}

