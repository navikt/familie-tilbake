package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class AvsluttBehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var avsluttBehandlingTask: AvsluttBehandlingTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `doTask skal avslutte behandling`() {
        var behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.IVERKSETTER_VEDTAK))
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandlingId,
                                                                           behandlingssteg = Behandlingssteg.AVSLUTTET,
                                                                           behandlingsstegsstatus = Behandlingsstegstatus.KLAR))

        avsluttBehandlingTask.doTask(Task(type = AvsluttBehandlingTask.TYPE, payload = behandlingId.toString()))

        behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        assertEquals(Behandlingsstatus.AVSLUTTET, behandling.status)

        val stegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(1, stegstilstand.size)
        assertEquals(Behandlingssteg.AVSLUTTET, stegstilstand[0].behandlingssteg)
        assertEquals(Behandlingsstegstatus.UTFØRT, stegstilstand[0].behandlingsstegsstatus)

        val tasker = taskRepository.findByStatus(Status.UBEHANDLET)
        val historikkTask = tasker.first { it.type == LagHistorikkinnslagTask.TYPE }
        assertEquals(LagHistorikkinnslagTask.TYPE, historikkTask.type)
        assertEquals(behandlingId.toString(), historikkTask.payload)
        val taskProperty = historikkTask.metadata
        assertEquals(Aktør.VEDTAKSLØSNING.name, taskProperty["aktor"])
        assertEquals(TilbakekrevingHistorikkinnslagstype.BEHANDLING_AVSLUTTET.name, taskProperty["historikkinnslagstype"])
    }
}
