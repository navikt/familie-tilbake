package no.nav.familie.tilbake.repository.tbd

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

internal class ForeldelsesperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var foreldelsesperiodeRepository: ForeldelsesperiodeRepository

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val foreldelsesperiode = Testdata.foreldelsesperiode

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        vurdertForeldelseRepository.insert(Testdata.vurdertForeldelse)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Foreldelsesperiode til basen`() {
        foreldelsesperiodeRepository.insert(foreldelsesperiode)

        val lagretForeldelsesperiode = foreldelsesperiodeRepository.findByIdOrThrow(foreldelsesperiode.id)

        assertThat(lagretForeldelsesperiode).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(foreldelsesperiode)
        assertEquals(1, lagretForeldelsesperiode.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Foreldelsesperiode i basen`() {
        foreldelsesperiodeRepository.insert(foreldelsesperiode)
        var lagretForeldelsesperiode = foreldelsesperiodeRepository.findByIdOrThrow(foreldelsesperiode.id)
        val oppdatertForeldelsesperiode = lagretForeldelsesperiode.copy(begrunnelse = "bob")

        foreldelsesperiodeRepository.update(oppdatertForeldelsesperiode)

        lagretForeldelsesperiode = foreldelsesperiodeRepository.findByIdOrThrow(foreldelsesperiode.id)
        assertThat(lagretForeldelsesperiode).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(oppdatertForeldelsesperiode)
        assertEquals(2, lagretForeldelsesperiode.versjon)
    }

}
