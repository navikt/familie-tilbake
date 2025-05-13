package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.data.Testdata.lagKravgrunnlagsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.august
import no.nav.tilbakekreving.desember
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.oktober
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PeriodeServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var periodeService: PeriodeService

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private lateinit var behandling: Behandling
    private lateinit var saksnummer: String
    private lateinit var fagsak: Fagsak

    private val førstePeriode = april(2020) til august(2020)
    private val andrePeriode = oktober(2020) til desember(2020)

    @BeforeEach
    fun setup() {
        fagsak = fagsakRepository.insert(Testdata.fagsak().copy(fagsystem = Fagsystem.EF, ytelsestype = Ytelsestype.OVERGANGSSTØNAD))
        behandling =
            Testdata.lagBehandling(fagsakId = fagsak.id).copy(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER,
                ansvarligBeslutter = ANSVARLIG_BESLUTTER,
                behandlendeEnhet = "8020",
            )

        behandling = behandlingRepository.insert(behandling)
        saksnummer = fagsak.eksternFagsakId
    }

    @Test
    fun `erEnsligForsørgerOgPerioderLike - en periode skal returnere IKKE_AKTUELL`() {
        faktaFeilutbetalingRepository.insert(Testdata.lagFaktaFeilutbetaling(behandling.id))
        val kravgrunnlag = lagKravgrunnlagsperiode(førstePeriode)
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id, setOf(kravgrunnlag)))
        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandling.id)
        erEnsligForsørgerOgPerioderLike shouldBe SkalSammenslåPerioder.IKKE_AKTUELT
    }

    @Test
    fun `erEnsligForsørgerOgPerioderLike - en periode som er splittet skal returnere IKKE_AKTUELL`() {
        faktaFeilutbetalingRepository.insert(Testdata.lagFaktaFeilutbetaling(behandling.id))
        val kravgrunnlag = lagKravgrunnlagsperiode(førstePeriode)
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id, setOf(kravgrunnlag)))

        val vilkårsvurderingsperiode = Vilkårsvurderingsperiode(periode = førstePeriode, vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, begrunnelse = "begrunnelse")
        vilkårsvurderingRepository.insert(Vilkårsvurdering(behandlingId = behandling.id, perioder = setOf(vilkårsvurderingsperiode)))

        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandling.id)
        erEnsligForsørgerOgPerioderLike shouldBe SkalSammenslåPerioder.IKKE_AKTUELT
    }

    @Test
    fun `erEnsligForsørgerOgPerioderLike - har to perioder i fakta - skal returnere JA`() {
        faktaFeilutbetalingRepository.insert(Testdata.lagFaktaFeilutbetaling(behandling.id, setOf(førstePeriode, andrePeriode)))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id, perioder = setOf(lagKravgrunnlagsperiode(førstePeriode), lagKravgrunnlagsperiode(andrePeriode))))
        foreldelseRepository.insert(Testdata.lagVurdertForeldelse(behandling.id, setOf(førstePeriode, andrePeriode)))

        val vilkårsvurderingsperiode = Vilkårsvurderingsperiode(periode = førstePeriode, vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, begrunnelse = "begrunnelse")
        val vilkårsvurderingsperiode2 = Vilkårsvurderingsperiode(periode = andrePeriode, vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, begrunnelse = "begrunnelse")
        vilkårsvurderingRepository.insert(Vilkårsvurdering(behandlingId = behandling.id, perioder = setOf(vilkårsvurderingsperiode, vilkårsvurderingsperiode2)))
        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandling.id)
        erEnsligForsørgerOgPerioderLike shouldBe SkalSammenslåPerioder.JA
    }

    @Test
    fun `erEnsligForsørgerOgPerioderLike - har to perioder i fakta for barnetilsyn - skal returnere JA`() {
        faktaFeilutbetalingRepository.insert(Testdata.lagFaktaFeilutbetaling(behandling.id, setOf(førstePeriode, andrePeriode)))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id, perioder = setOf(lagKravgrunnlagsperiode(førstePeriode, Klassekode.EFBT), lagKravgrunnlagsperiode(andrePeriode, Klassekode.EFBT)), fagområdekode = Fagområdekode.EFBT))
        foreldelseRepository.insert(Testdata.lagVurdertForeldelse(behandling.id, setOf(førstePeriode, andrePeriode)))

        val vilkårsvurderingsperiode = Vilkårsvurderingsperiode(periode = førstePeriode, vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, begrunnelse = "begrunnelse")
        val vilkårsvurderingsperiode2 = Vilkårsvurderingsperiode(periode = andrePeriode, vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, begrunnelse = "begrunnelse")
        vilkårsvurderingRepository.insert(Vilkårsvurdering(behandlingId = behandling.id, perioder = setOf(vilkårsvurderingsperiode, vilkårsvurderingsperiode2)))
        val erEnsligForsørgerOgPerioderLike = periodeService.erEnsligForsørgerOgPerioderLike(behandling.id)
        erEnsligForsørgerOgPerioderLike shouldBe SkalSammenslåPerioder.JA
    }

    companion object {
        private const val ANSVARLIG_SAKSBEHANDLER = "Z13456"
        private const val ANSVARLIG_BESLUTTER = "Z12456"
    }
}
