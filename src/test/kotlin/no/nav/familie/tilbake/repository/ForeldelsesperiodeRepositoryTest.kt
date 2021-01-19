package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
    fun insertPersistererEnForekomstAvForeldelsesperiodeTilBasen() {
        foreldelsesperiodeRepository.insert(foreldelsesperiode)

        val lagretForeldelsesperiode = foreldelsesperiodeRepository.findByIdOrThrow(foreldelsesperiode.id)

        Assertions.assertThat(lagretForeldelsesperiode).isEqualToIgnoringGivenFields(foreldelsesperiode, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvForeldelsesperiodeIBasen() {
        foreldelsesperiodeRepository.insert(foreldelsesperiode)
        val oppdatertForeldelsesperiode = foreldelsesperiode.copy(begrunnelse = "bob")

        foreldelsesperiodeRepository.update(oppdatertForeldelsesperiode)

        val lagretForeldelsesperiode = foreldelsesperiodeRepository.findByIdOrThrow(foreldelsesperiode.id)
        Assertions.assertThat(lagretForeldelsesperiode).isEqualToIgnoringGivenFields(oppdatertForeldelsesperiode, "sporbar")
    }

}