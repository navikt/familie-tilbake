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
    private lateinit var grupperingFaktaFeilutbetalingRepository: GrupperingFaktaFeilutbetalingRepository

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
        grupperingFaktaFeilutbetalingRepository.insert(Testdata.grupperingFaktaFeilutbetaling)
        vurdertForeldelseRepository.insert(Testdata.vurdertForeldelse)
        grupperingVurdertForeldelseRepository.insert(Testdata.grupperingVurdertForeldelse)
        vilkårsvurderingRepository.insert(Testdata.vilkår)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Totrinnsresultatsgrunnlag til basen`() {
        totrinnsresultatsgrunnlagRepository.insert(totrinnsresultatsgrunnlag)

        val lagretTotrinnsresultatsgrunnlag = totrinnsresultatsgrunnlagRepository.findByIdOrThrow(totrinnsresultatsgrunnlag.id)

        Assertions.assertThat(lagretTotrinnsresultatsgrunnlag).isEqualToIgnoringGivenFields(totrinnsresultatsgrunnlag, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Totrinnsresultatsgrunnlag i basen`() {
        totrinnsresultatsgrunnlagRepository.insert(totrinnsresultatsgrunnlag)
        val oppdatertTotrinnsresultatsgrunnlag = totrinnsresultatsgrunnlag.copy(aktiv = false)

        totrinnsresultatsgrunnlagRepository.update(oppdatertTotrinnsresultatsgrunnlag)

        val lagretTotrinnsresultatsgrunnlag = totrinnsresultatsgrunnlagRepository.findByIdOrThrow(totrinnsresultatsgrunnlag.id)
        Assertions.assertThat(lagretTotrinnsresultatsgrunnlag)
                .isEqualToIgnoringGivenFields(oppdatertTotrinnsresultatsgrunnlag, "sporbar")
    }

}
