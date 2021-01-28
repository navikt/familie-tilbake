package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Behandlingstegsstatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsstegstilstandRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val behandlingsstegstilstand = Testdata.behandlingsstegstilstand

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Behandlingsstegstilstand til basen`() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)

        val lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)

        Assertions.assertThat(lagretBehandlingsstegstilstand).isEqualToIgnoringGivenFields(behandlingsstegstilstand, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Behandlingsstegstilstand i basen`() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)
        val oppdatertBehandlingsstegstilstand =
                behandlingsstegstilstand.copy(behandlingsstegsstatus = Behandlingstegsstatus.STARTET)

        behandlingsstegstilstandRepository.update(oppdatertBehandlingsstegstilstand)

        val lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)
        Assertions.assertThat(lagretBehandlingsstegstilstand)
                .isEqualToIgnoringGivenFields(oppdatertBehandlingsstegstilstand, "sporbar")
    }

}