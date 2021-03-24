package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Venteårsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

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
        assertThat(lagretAksjonspunkt).usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(aksjonspunkt)
        assertEquals(1, lagretAksjonspunkt.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Aksjonspunkt i basen`() {
        aksjonspunktRepository.insert(aksjonspunkt)
        var lagretAksjonspunkt = aksjonspunktRepository.findByIdOrThrow(aksjonspunkt.id)
        val oppdatertAksjonspunkt = lagretAksjonspunkt.copy(ventearsak = Venteårsak.ENDRE_TILKJENT_YTELSE)

        aksjonspunktRepository.update(oppdatertAksjonspunkt)

        lagretAksjonspunkt = aksjonspunktRepository.findByIdOrThrow(aksjonspunkt.id)
        assertThat(lagretAksjonspunkt).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(oppdatertAksjonspunkt)
        assertEquals(2, lagretAksjonspunkt.versjon)
    }

}
