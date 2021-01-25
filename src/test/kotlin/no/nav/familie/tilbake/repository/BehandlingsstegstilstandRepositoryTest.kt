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
    fun insertPersistererEnForekomstAvBehandlingsstegstilstandTilBasen() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)

        val lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)

        Assertions.assertThat(lagretBehandlingsstegstilstand).isEqualToIgnoringGivenFields(behandlingsstegstilstand, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBehandlingsstegstilstandIBasen() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)
        val oppdatertBehandlingsstegstilstand =
                behandlingsstegstilstand.copy(behandlingsstegsstatus = Behandlingstegsstatus.STARTET)

        behandlingsstegstilstandRepository.update(oppdatertBehandlingsstegstilstand)

        val lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)
        Assertions.assertThat(lagretBehandlingsstegstilstand)
                .isEqualToIgnoringGivenFields(oppdatertBehandlingsstegstilstand, "sporbar")
    }

}