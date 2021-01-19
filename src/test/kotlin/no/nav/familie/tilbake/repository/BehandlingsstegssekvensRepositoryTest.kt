package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsstegssekvensRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsstegssekvensRepository: BehandlingsstegssekvensRepository

    @Autowired
    private lateinit var behandlingsstegstypeRepository: BehandlingsstegstypeRepository

    private val behandlingsstegssekvens = Testdata.behandlingsstegssekvens

    @BeforeEach
    fun init() {
        behandlingsstegstypeRepository.insert(Testdata.behandlingsstegstype)
    }

    @Test
    fun insertPersistererEnForekomstAvBehandlingsstegssekvensTilBasen() {
        behandlingsstegssekvensRepository.insert(behandlingsstegssekvens)

        val lagretBehandlingsstegssekvens = behandlingsstegssekvensRepository.findByIdOrThrow(behandlingsstegssekvens.id)

        Assertions.assertThat(lagretBehandlingsstegssekvens).isEqualToIgnoringGivenFields(behandlingsstegssekvens, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBehandlingsstegssekvensIBasen() {
        behandlingsstegssekvensRepository.insert(behandlingsstegssekvens)
        val oppdatertBehandlingsstegssekvens = behandlingsstegssekvens.copy(behandlingstype = "bob")

        behandlingsstegssekvensRepository.update(oppdatertBehandlingsstegssekvens)

        val lagretBehandlingsstegssekvens = behandlingsstegssekvensRepository.findByIdOrThrow(behandlingsstegssekvens.id)
        Assertions.assertThat(lagretBehandlingsstegssekvens)
                .isEqualToIgnoringGivenFields(oppdatertBehandlingsstegssekvens, "sporbar")
    }

}