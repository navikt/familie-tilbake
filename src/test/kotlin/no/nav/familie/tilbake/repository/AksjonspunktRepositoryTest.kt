package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AksjonspunktRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var aksjonspunktRepository: AksjonspunktRepository

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

    private val aksjonspunkt = Testdata.aksjonspunkt

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        behandlingsstegstypeRepository.insert(Testdata.behandlingsstegstype)
        vurderingspunktsdefinisjonRepository.insert(Testdata.vurderingspunktsdefinisjon)
        aksjonspunktsdefinisjonRepository.insert(Testdata.aksjonspunktsdefinisjon)
    }

    @Test
    fun insertPersistererEnForekomstAvAksjonspunktTilBasen() {
        aksjonspunktRepository.insert(aksjonspunkt)

        val lagretAksjonspunkt = aksjonspunktRepository.findByIdOrThrow(aksjonspunkt.id)

        Assertions.assertThat(lagretAksjonspunkt).isEqualToIgnoringGivenFields(aksjonspunkt, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvAksjonspunktIBasen() {
        aksjonspunktRepository.insert(aksjonspunkt)
        val oppdatertAksjonspunkt = aksjonspunkt.copy(ventearsak = "bob")

        aksjonspunktRepository.update(oppdatertAksjonspunkt)

        val lagretAksjonspunkt = aksjonspunktRepository.findByIdOrThrow(aksjonspunkt.id)
        Assertions.assertThat(lagretAksjonspunkt).isEqualToIgnoringGivenFields(oppdatertAksjonspunkt, "sporbar")
    }

}