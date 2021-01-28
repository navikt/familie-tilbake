package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.repository.tbd.GrupperingFaktaFeilutbetalingRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

internal class GrupperingFaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var grupperingFaktaFeilutbetalingRepository: GrupperingFaktaFeilutbetalingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    private val grupperingFaktaFeilutbetaling = Testdata.grupperingFaktaFeilutbetaling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        faktaFeilutbetalingRepository.insert(Testdata.faktaFeilutbetaling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av GrupperingFaktaFeilutbetaling til basen`() {
        grupperingFaktaFeilutbetalingRepository.insert(grupperingFaktaFeilutbetaling)

        val lagretGrupperingFaktaFeilutbetaling =
                grupperingFaktaFeilutbetalingRepository.findByIdOrThrow(grupperingFaktaFeilutbetaling.id)

        Assertions.assertThat(lagretGrupperingFaktaFeilutbetaling)
                .isEqualToIgnoringGivenFields(grupperingFaktaFeilutbetaling, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av GrupperingFaktaFeilutbetaling i basen`() {
        grupperingFaktaFeilutbetalingRepository.insert(grupperingFaktaFeilutbetaling)
        val oppdatertGrupperingFaktaFeilutbetaling = grupperingFaktaFeilutbetaling.copy(aktiv = false)

        grupperingFaktaFeilutbetalingRepository.update(oppdatertGrupperingFaktaFeilutbetaling)

        val lagretGrupperingFaktaFeilutbetaling =
                grupperingFaktaFeilutbetalingRepository.findByIdOrThrow(grupperingFaktaFeilutbetaling.id)
        Assertions.assertThat(lagretGrupperingFaktaFeilutbetaling)
                .isEqualToIgnoringGivenFields(oppdatertGrupperingFaktaFeilutbetaling,
                                              "sporbar")
    }

}