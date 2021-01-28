package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
        val oppdatertBehandling = behandling.copy(status = Behandlingsstatus.UTREDES)

        behandlingRepository.update(oppdatertBehandling)

        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        Assertions.assertThat(lagretBehandling).isEqualToIgnoringGivenFields(oppdatertBehandling, "sporbar")
    }

}
