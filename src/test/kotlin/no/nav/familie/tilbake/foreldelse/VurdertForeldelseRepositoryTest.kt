package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

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
    fun `insert med gyldige verdier skal persistere en forekomst av VurdertForeldelse til basen`() {
        vurdertForeldelseRepository.insert(vurdertForeldelse)

        val lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)

        assertThat(lagretVurdertForeldelse).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(vurdertForeldelse)
        assertEquals(1, lagretVurdertForeldelse.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av VurdertForeldelse i basen`() {
        vurdertForeldelseRepository.insert(vurdertForeldelse)
        var lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)
        val oppdatertVurdertForeldelse = lagretVurdertForeldelse.copy(aktiv = false)

        vurdertForeldelseRepository.update(oppdatertVurdertForeldelse)

        lagretVurdertForeldelse = vurdertForeldelseRepository.findByIdOrThrow(vurdertForeldelse.id)
        assertThat(lagretVurdertForeldelse).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(oppdatertVurdertForeldelse)
        assertEquals(2, lagretVurdertForeldelse.versjon)
    }

}
