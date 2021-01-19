package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsårsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsårsakRepository: BehandlingsårsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val behandlingsårsak = Testdata.behandlingsårsak

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun insertPersistererEnForekomstAvBehandlingsårsakTilBasen() {
        behandlingsårsakRepository.insert(behandlingsårsak)

        val lagretBehandlingsårsak = behandlingsårsakRepository.findByIdOrThrow(behandlingsårsak.id)

        Assertions.assertThat(lagretBehandlingsårsak).isEqualToIgnoringGivenFields(behandlingsårsak, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBehandlingsårsakIBasen() {
        behandlingsårsakRepository.insert(behandlingsårsak)
        val oppdatertBehandlingsårsak = behandlingsårsak.copy(type = "bob")

        behandlingsårsakRepository.update(oppdatertBehandlingsårsak)

        val lagretBehandlingsårsak = behandlingsårsakRepository.findByIdOrThrow(behandlingsårsak.id)
        Assertions.assertThat(lagretBehandlingsårsak).isEqualToIgnoringGivenFields(oppdatertBehandlingsårsak, "sporbar")
    }

}