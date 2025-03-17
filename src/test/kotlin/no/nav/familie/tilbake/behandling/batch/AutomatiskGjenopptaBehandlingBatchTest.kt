package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class AutomatiskGjenopptaBehandlingBatchTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var automatiskGjenopptaBehandlingBatch: AutomatiskGjenopptaBehandlingBatch

    @Test
    fun `skal lage task på behandling som venter på varsel og tidsfristen har utgått`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(status = Behandlingsstatus.UTREDES))
        behandlingsstegstilstandRepository.insert(
            Testdata.lagBehandlingsstegstilstand(behandling.id).copy(
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().minusWeeks(4),
            ),
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        taskService
            .findAll()
            .any {
                it.type == AutomatiskGjenopptaBehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeTrue()
    }

    @Test
    fun `skal lage task på behandling som venter på avvent dokumentasjon`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(status = Behandlingsstatus.UTREDES))
        val tidsfrist = LocalDate.now().minusWeeks(1)
        behandlingsstegstilstandRepository.insert(
            Testdata.lagBehandlingsstegstilstand(behandling.id).copy(
                behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.AVVENTER_DOKUMENTASJON,
                tidsfrist = tidsfrist,
            ),
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        taskService
            .findAll()
            .any {
                it.type == AutomatiskGjenopptaBehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeTrue()
    }
}
