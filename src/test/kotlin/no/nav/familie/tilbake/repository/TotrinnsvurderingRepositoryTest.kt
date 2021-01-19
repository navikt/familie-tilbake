package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TotrinnsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var totrinnsvurderingRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var aksjonspunktsdefinisjonRepository: AksjonspunktsdefinisjonRepository

    @Autowired
    private lateinit var vurderingspunktsdefinisjonRepository: VurderingspunktsdefinisjonRepository

    @Autowired
    private lateinit var behandlingsstegstypeRepository: BehandlingsstegstypeRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val totrinnsvurdering = Testdata.totrinnsvurdering

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        behandlingsstegstypeRepository.insert(Testdata.behandlingsstegstype)
        vurderingspunktsdefinisjonRepository.insert(Testdata.vurderingspunktsdefinisjon)
        aksjonspunktsdefinisjonRepository.insert(Testdata.aksjonspunktsdefinisjon)
    }

    @Test
    fun insertPersistererEnForekomstAvTotrinnsvurderingTilBasen() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)

        val lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)

        Assertions.assertThat(lagretTotrinnsvurdering).isEqualToIgnoringGivenFields(totrinnsvurdering, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvTotrinnsvurderingIBasen() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurdering.copy(begrunnelse = "bob")

        totrinnsvurderingRepository.update(oppdatertTotrinnsvurdering)

        val lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)
        Assertions.assertThat(lagretTotrinnsvurdering).isEqualToIgnoringGivenFields(oppdatertTotrinnsvurdering, "sporbar")
    }

}