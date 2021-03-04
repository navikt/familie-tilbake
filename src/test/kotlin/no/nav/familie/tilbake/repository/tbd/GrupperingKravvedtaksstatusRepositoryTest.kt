package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravvedtaksstatusRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class GrupperingKravvedtaksstatusRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var grupperingKravvedtaksstatusRepository: GrupperingKravvedtaksstatusRepository

    @Autowired
    private lateinit var kravvedtaksstatusRepository: KravvedtaksstatusRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val grupperingKravvedtaksstatus = Testdata.grupperingKravvedtaksstatus

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        kravvedtaksstatusRepository.insert(Testdata.kravvedtaksstatus437)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av GrupperingKravvedtaksstatus til basen`() {
        grupperingKravvedtaksstatusRepository.insert(grupperingKravvedtaksstatus)

        val lagretGrupperingKravvedtaksstatus =
                grupperingKravvedtaksstatusRepository.findByIdOrThrow(grupperingKravvedtaksstatus.id)

        assertThat(lagretGrupperingKravvedtaksstatus)
                .isEqualToIgnoringGivenFields(grupperingKravvedtaksstatus, "sporbar", "versjon")
        assertEquals(1, lagretGrupperingKravvedtaksstatus.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av GrupperingKravvedtaksstatus i basen`() {
        grupperingKravvedtaksstatusRepository.insert(grupperingKravvedtaksstatus)
        var lagretGrupperingKravvedtaksstatus =
                grupperingKravvedtaksstatusRepository.findByIdOrThrow(grupperingKravvedtaksstatus.id)
        val oppdatertGrupperingKravvedtaksstatus = lagretGrupperingKravvedtaksstatus.copy(aktiv = false)

        grupperingKravvedtaksstatusRepository.update(oppdatertGrupperingKravvedtaksstatus)

        lagretGrupperingKravvedtaksstatus =
                grupperingKravvedtaksstatusRepository.findByIdOrThrow(grupperingKravvedtaksstatus.id)
        assertThat(lagretGrupperingKravvedtaksstatus)
                .isEqualToIgnoringGivenFields(oppdatertGrupperingKravvedtaksstatus,
                                              "sporbar", "versjon")
        assertEquals(2, lagretGrupperingKravvedtaksstatus.versjon)
    }

}
