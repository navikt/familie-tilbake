package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class AutomatiskBehandlingRydderBatchTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var automatiskBehandlingRydderBatch: AutomatiskBehandlingRydderBatch

    @Autowired
    private lateinit var taskService: TaskService

    @Test
    fun `skal lage task på behandling uten kravgunnlag og 8 uker gammel`() {
        val fagsak = Testdata.fagsak
        fagsakRepository.insert(fagsak)

        val behandling =
            behandlingRepository.insert(
                Testdata.lagBehandling().copy(
                    status = Behandlingsstatus.UTREDES,
                    opprettetDato = LocalDate.now().minusWeeks(9),
                ),
            )

        // rydder behandlinger eldre enn 8 uker med status ikke avslutning
        shouldNotThrow<RuntimeException> { automatiskBehandlingRydderBatch.automatiskFjerningAvGammelBehandlingerUtenKravgrunnlag() }

        taskService
            .findAll()
            .any {
                it.type == RyddBehandlingUtenKravgrunnlagTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeTrue()
    }
}
