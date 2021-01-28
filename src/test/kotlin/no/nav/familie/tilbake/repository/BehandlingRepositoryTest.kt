package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Behandling til basen`() {
        behandlingRepository.insert(behandling)

        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        Assertions.assertThat(lagretBehandling).isEqualToIgnoringGivenFields(behandling, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Behandling i basen`() {
        behandlingRepository.insert(behandling)
        val oppdatertBehandling = behandling.copy(opprettetDato = LocalDate.now().minusDays(15))

        behandlingRepository.update(oppdatertBehandling)

        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        Assertions.assertThat(lagretBehandling).isEqualToIgnoringGivenFields(oppdatertBehandling, "sporbar")
    }

}