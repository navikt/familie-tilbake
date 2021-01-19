package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VurdertForeldelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val vurdertForeldelse = Testdata.vurdertForeldelse

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun insertPersistererEnForekomstAvVurdertForeldelseTilBasen() {
        vurdertForeldelseRepository.insert(vurdertForeldelse)

        val lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)

        Assertions.assertThat(lagretVurdertForeldelse).isEqualToIgnoringGivenFields(vurdertForeldelse, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVurdertForeldelseIBasen() {
        vurdertForeldelseRepository.insert(vurdertForeldelse)
        val oppdatertVurdertForeldelse = vurdertForeldelse.copy(aktiv = false)

        vurdertForeldelseRepository.update(oppdatertVurdertForeldelse)

        val lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)
        Assertions.assertThat(lagretVurdertForeldelse).isEqualToIgnoringGivenFields(oppdatertVurdertForeldelse, "sporbar")
    }

}