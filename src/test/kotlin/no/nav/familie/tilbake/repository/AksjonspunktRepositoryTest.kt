package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Venteårsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AksjonspunktRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var aksjonspunktRepository: AksjonspunktRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val aksjonspunkt = Testdata.aksjonspunkt

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Aksjonspunkt til basen`() {
        aksjonspunktRepository.insert(aksjonspunkt)

        val lagretAksjonspunkt = aksjonspunktRepository.findByIdOrThrow(aksjonspunkt.id)

        Assertions.assertThat(lagretAksjonspunkt).isEqualToIgnoringGivenFields(aksjonspunkt, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Aksjonspunkt i basen`() {
        aksjonspunktRepository.insert(aksjonspunkt)
        val oppdatertAksjonspunkt = aksjonspunkt.copy(ventearsak = Venteårsak.ENDRE_TILKJENT_YTELSE)

        aksjonspunktRepository.update(oppdatertAksjonspunkt)

        val lagretAksjonspunkt = aksjonspunktRepository.findByIdOrThrow(aksjonspunkt.id)
        Assertions.assertThat(lagretAksjonspunkt).isEqualToIgnoringGivenFields(oppdatertAksjonspunkt, "sporbar")
    }

}