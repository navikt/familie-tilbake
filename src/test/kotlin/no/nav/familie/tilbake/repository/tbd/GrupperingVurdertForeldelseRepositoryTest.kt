package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrupperingVurdertForeldelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var grupperingVurdertForeldelseRepository: GrupperingVurdertForeldelseRepository

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val grupperingVurdertForeldelse = Testdata.grupperingVurdertForeldelse

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        vurdertForeldelseRepository.insert(Testdata.vurdertForeldelse)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av GrupperingVurdertForeldelse til basen`() {
        grupperingVurdertForeldelseRepository.insert(grupperingVurdertForeldelse)

        val lagretGrupperingVurdertForeldelse =
                grupperingVurdertForeldelseRepository.findByIdOrThrow(grupperingVurdertForeldelse.id)

        Assertions.assertThat(lagretGrupperingVurdertForeldelse)
                .isEqualToIgnoringGivenFields(grupperingVurdertForeldelse, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av GrupperingVurdertForeldelse i basen`() {
        grupperingVurdertForeldelseRepository.insert(grupperingVurdertForeldelse)
        val oppdatertGrupperingVurdertForeldelse = grupperingVurdertForeldelse.copy(aktiv = false)

        grupperingVurdertForeldelseRepository.update(oppdatertGrupperingVurdertForeldelse)

        val lagretGrupperingVurdertForeldelse =
                grupperingVurdertForeldelseRepository.findByIdOrThrow(grupperingVurdertForeldelse.id)
        Assertions.assertThat(lagretGrupperingVurdertForeldelse)
                .isEqualToIgnoringGivenFields(oppdatertGrupperingVurdertForeldelse,
                                              "sporbar")
    }

}
