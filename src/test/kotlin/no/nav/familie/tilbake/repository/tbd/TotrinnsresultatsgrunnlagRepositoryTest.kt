package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class TotrinnsresultatsgrunnlagRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var totrinnsresultatsgrunnlagRepository: TotrinnsresultatsgrunnlagRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var grupperingVurdertForeldelseRepository: GrupperingVurdertForeldelseRepository

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val totrinnsresultatsgrunnlag = Testdata.totrinnsresultatsgrunnlag

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        faktaFeilutbetalingRepository.insert(Testdata.faktaFeilutbetaling)
        vurdertForeldelseRepository.insert(Testdata.vurdertForeldelse)
        grupperingVurdertForeldelseRepository.insert(Testdata.grupperingVurdertForeldelse)
        vilkårsvurderingRepository.insert(Testdata.vilkår)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Totrinnsresultatsgrunnlag til basen`() {
        totrinnsresultatsgrunnlagRepository.insert(totrinnsresultatsgrunnlag)

        val lagretTotrinnsresultatsgrunnlag = totrinnsresultatsgrunnlagRepository.findByIdOrThrow(totrinnsresultatsgrunnlag.id)

        Assertions.assertThat(lagretTotrinnsresultatsgrunnlag).isEqualToIgnoringGivenFields(totrinnsresultatsgrunnlag,
                                                                                            "sporbar", "versjon")
        assertEquals(1, lagretTotrinnsresultatsgrunnlag.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Totrinnsresultatsgrunnlag i basen`() {
        totrinnsresultatsgrunnlagRepository.insert(totrinnsresultatsgrunnlag)
        var lagretTotrinnsresultatsgrunnlag = totrinnsresultatsgrunnlagRepository.findByIdOrThrow(totrinnsresultatsgrunnlag.id)
        val oppdatertTotrinnsresultatsgrunnlag = lagretTotrinnsresultatsgrunnlag.copy(aktiv = false)

        totrinnsresultatsgrunnlagRepository.update(oppdatertTotrinnsresultatsgrunnlag)

        lagretTotrinnsresultatsgrunnlag = totrinnsresultatsgrunnlagRepository.findByIdOrThrow(totrinnsresultatsgrunnlag.id)
        Assertions.assertThat(lagretTotrinnsresultatsgrunnlag)
                .isEqualToIgnoringGivenFields(oppdatertTotrinnsresultatsgrunnlag, "sporbar", "versjon")
        assertEquals(2, lagretTotrinnsresultatsgrunnlag.versjon)
    }

}
