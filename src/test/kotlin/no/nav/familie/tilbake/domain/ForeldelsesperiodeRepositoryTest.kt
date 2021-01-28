package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.ForeldelsesperiodeRepository
import no.nav.familie.tilbake.repository.tbd.VurdertForeldelseRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
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

        Assertions.assertThat(lagretForeldelsesperiode).isEqualToIgnoringGivenFields(foreldelsesperiode, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Foreldelsesperiode i basen`() {
        foreldelsesperiodeRepository.insert(foreldelsesperiode)
        val oppdatertForeldelsesperiode = foreldelsesperiode.copy(begrunnelse = "bob")

        foreldelsesperiodeRepository.update(oppdatertForeldelsesperiode)

        val lagretForeldelsesperiode = foreldelsesperiodeRepository.findByIdOrThrow(foreldelsesperiode.id)
        Assertions.assertThat(lagretForeldelsesperiode).isEqualToIgnoringGivenFields(oppdatertForeldelsesperiode, "sporbar")
    }

}