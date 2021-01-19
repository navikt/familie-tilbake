package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class EksternBehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var eksternBehandlingRepository: EksternBehandlingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val eksternBehandling = Testdata.eksternBehandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun insertPersistererEnForekomstAvEksternBehandlingTilBasen() {
        eksternBehandlingRepository.insert(eksternBehandling)

        val lagretEksternBehandling = eksternBehandlingRepository.findByIdOrThrow(eksternBehandling.id)

        Assertions.assertThat(lagretEksternBehandling).isEqualToIgnoringGivenFields(eksternBehandling, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvEksternBehandlingIBasen() {
        eksternBehandlingRepository.insert(eksternBehandling)
        val oppdatertEksternBehandling = eksternBehandling.copy(henvisning = "bob")

        eksternBehandlingRepository.update(oppdatertEksternBehandling)

        val lagretEksternBehandling = eksternBehandlingRepository.findByIdOrThrow(eksternBehandling.id)
        Assertions.assertThat(lagretEksternBehandling).isEqualToIgnoringGivenFields(oppdatertEksternBehandling, "sporbar")
    }

}