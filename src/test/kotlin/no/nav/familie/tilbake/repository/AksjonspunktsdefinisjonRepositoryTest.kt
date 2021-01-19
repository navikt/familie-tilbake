package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AksjonspunktsdefinisjonRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var aksjonspunktsdefinisjonRepository: AksjonspunktsdefinisjonRepository

    @Autowired
    private lateinit var vurderingspunktsdefinisjonRepository: VurderingspunktsdefinisjonRepository

    @Autowired
    private lateinit var behandlingsstegstypeRepository: BehandlingsstegstypeRepository

    private val aksjonspunktsdefinisjon = Testdata.aksjonspunktsdefinisjon

    @BeforeEach
    fun init() {
        behandlingsstegstypeRepository.insert(Testdata.behandlingsstegstype)
        vurderingspunktsdefinisjonRepository.insert(Testdata.vurderingspunktsdefinisjon)
    }

    @Test
    fun insertPersistererEnForekomstAvAksjonspunktsdefinisjonTilBasen() {
        aksjonspunktsdefinisjonRepository.insert(aksjonspunktsdefinisjon)

        val lagretAksjonspunktsdefinisjon = aksjonspunktsdefinisjonRepository.findByIdOrThrow(aksjonspunktsdefinisjon.id)

        Assertions.assertThat(lagretAksjonspunktsdefinisjon).isEqualToIgnoringGivenFields(aksjonspunktsdefinisjon, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvAksjonspunktsdefinisjonIBasen() {
        aksjonspunktsdefinisjonRepository.insert(aksjonspunktsdefinisjon)
        val oppdatertAksjonspunktsdefinisjon = aksjonspunktsdefinisjon.copy(navn = "bob")

        aksjonspunktsdefinisjonRepository.update(oppdatertAksjonspunktsdefinisjon)

        val lagretAksjonspunktsdefinisjon = aksjonspunktsdefinisjonRepository.findByIdOrThrow(aksjonspunktsdefinisjon.id)
        Assertions.assertThat(lagretAksjonspunktsdefinisjon)
                .isEqualToIgnoringGivenFields(oppdatertAksjonspunktsdefinisjon, "sporbar")
    }

}