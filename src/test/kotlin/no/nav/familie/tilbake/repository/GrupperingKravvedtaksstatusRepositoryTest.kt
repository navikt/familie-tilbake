package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrupperingKravvedtaksstatusRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var grupperingKravvedtaksstatusRepository: GrupperingKravvedtaksstatusRepository

    @Autowired
    private lateinit var kravvedtaksstatus437Repository: Kravvedtaksstatus437Repository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val grupperingKravvedtaksstatus = Testdata.grupperingKravvedtaksstatus

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        kravvedtaksstatus437Repository.insert(Testdata.kravvedtaksstatus437)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av GrupperingKravvedtaksstatus til basen`() {
        grupperingKravvedtaksstatusRepository.insert(grupperingKravvedtaksstatus)

        val lagretGrupperingKravvedtaksstatus =
                grupperingKravvedtaksstatusRepository.findByIdOrThrow(grupperingKravvedtaksstatus.id)

        Assertions.assertThat(lagretGrupperingKravvedtaksstatus)
                .isEqualToIgnoringGivenFields(grupperingKravvedtaksstatus, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av GrupperingKravvedtaksstatus i basen`() {
        grupperingKravvedtaksstatusRepository.insert(grupperingKravvedtaksstatus)
        val oppdatertGrupperingKravvedtaksstatus = grupperingKravvedtaksstatus.copy(aktiv = false)

        grupperingKravvedtaksstatusRepository.update(oppdatertGrupperingKravvedtaksstatus)

        val lagretGrupperingKravvedtaksstatus =
                grupperingKravvedtaksstatusRepository.findByIdOrThrow(grupperingKravvedtaksstatus.id)
        Assertions.assertThat(lagretGrupperingKravvedtaksstatus)
                .isEqualToIgnoringGivenFields(oppdatertGrupperingKravvedtaksstatus,
                                              "sporbar")
    }

}