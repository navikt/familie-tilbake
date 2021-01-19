package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsresultatRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val behandlingsresultat = Testdata.behandlingsresultat

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun insertPersistererEnForekomstAvBehandlingsresultatTilBasen() {
        behandlingsresultatRepository.insert(behandlingsresultat)

        val lagretBehandlingsresultat = behandlingsresultatRepository.findByIdOrThrow(behandlingsresultat.id)

        Assertions.assertThat(lagretBehandlingsresultat).isEqualToIgnoringGivenFields(behandlingsresultat, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBehandlingsresultatIBasen() {
        behandlingsresultatRepository.insert(behandlingsresultat)
        val oppdatertBehandlingsresultat = behandlingsresultat.copy(versjon = 15)

        behandlingsresultatRepository.update(oppdatertBehandlingsresultat)

        val lagretBehandlingsresultat = behandlingsresultatRepository.findByIdOrThrow(behandlingsresultat.id)
        Assertions.assertThat(lagretBehandlingsresultat).isEqualToIgnoringGivenFields(oppdatertBehandlingsresultat, "sporbar")
    }

}