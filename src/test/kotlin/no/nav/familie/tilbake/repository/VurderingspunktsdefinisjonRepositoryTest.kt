package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VurderingspunktsdefinisjonRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vurderingspunktsdefinisjonRepository: VurderingspunktsdefinisjonRepository

    @Autowired
    private lateinit var behandlingsstegstypeRepository: BehandlingsstegstypeRepository

    private val vurderingspunktsdefinisjon = Testdata.vurderingspunktsdefinisjon

    @BeforeEach
    fun init() {
        behandlingsstegstypeRepository.insert(Testdata.behandlingsstegstype)
    }

    @Test
    fun insertPersistererEnForekomstAvVurderingspunktsdefinisjonTilBasen() {
        vurderingspunktsdefinisjonRepository.insert(vurderingspunktsdefinisjon)

        val lagretVurderingspunktsdefinisjon = vurderingspunktsdefinisjonRepository.findByIdOrThrow(vurderingspunktsdefinisjon.id)

        Assertions.assertThat(lagretVurderingspunktsdefinisjon)
                .isEqualToIgnoringGivenFields(vurderingspunktsdefinisjon, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVurderingspunktsdefinisjonIBasen() {
        vurderingspunktsdefinisjonRepository.insert(vurderingspunktsdefinisjon)
        val oppdatertVurderingspunktsdefinisjon = vurderingspunktsdefinisjon.copy(navn = "bob")

        vurderingspunktsdefinisjonRepository.update(oppdatertVurderingspunktsdefinisjon)

        val lagretVurderingspunktsdefinisjon = vurderingspunktsdefinisjonRepository.findByIdOrThrow(vurderingspunktsdefinisjon.id)
        Assertions.assertThat(lagretVurderingspunktsdefinisjon)
                .isEqualToIgnoringGivenFields(oppdatertVurderingspunktsdefinisjon, "sporbar")
    }

}