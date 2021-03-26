package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Aktsomhet
import no.nav.familie.tilbake.domain.tbd.AnnenVurdering
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurdering
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsresultat
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.repository.tbd.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TilbakekrevingsberegningServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tilbakekrevingsberegningService: TilbakekrevingsberegningService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var vurdertForeldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository


    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)

    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som ikke er foreldet`() {
        val periode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.periode).isEqualTo(periode)
        assertThat(r.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(r.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(r.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(r.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(r.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(r.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(beregningsresultat.vedtaksresultat).isEqualByComparingTo(Vedtaksresultat.FULL_TILBAKEBETALING)
    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som gjelder ikke er foreldelse`() {
        val periode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.periode).isEqualTo(periode)
        assertThat(r.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(r.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(r.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(r.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(r.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(r.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(beregningsresultat.vedtaksresultat).isEqualByComparingTo(Vedtaksresultat.FULL_TILBAKEBETALING)
    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som er foreldet`() {
        val periode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.ZERO)
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.FORELDET, periode.fom.plusMonths(8).atDay(1))
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.periode).isEqualTo(periode)
        assertThat(r.tilbakekrevingsbeløp).isZero()
        assertThat(r.vurdering).isEqualTo(AnnenVurdering.FORELDET)
        assertThat(r.renteprosent).isNull()
        assertThat(r.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(r.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(r.andelAvBeløp).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(r.rentebeløp).isZero()
        assertThat(r.tilbakekrevingsbeløpUtenRenter).isZero()
        assertThat(beregningsresultat.vedtaksresultat).isEqualByComparingTo(Vedtaksresultat.INGEN_TILBAKEBETALING)
    }

    @Test
    fun `beregn skalberegne tilbakekrevingsbeløp for periode som ikke er foreldet medSkattProsent`() {
        val periode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.valueOf(10))
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.periode).isEqualTo(periode)
        assertThat(r.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(r.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(r.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(r.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(r.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(r.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(r.skattebeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(r.tilbakekrevingsbeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(beregningsresultat.vedtaksresultat).isEqualByComparingTo(Vedtaksresultat.FULL_TILBAKEBETALING)
    }

    @Test
    fun `beregn skalberegne riktig beløp og utbetalt beløp for periode`() {
        val periode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        lagKravgrunnlag(periode, BigDecimal.valueOf(10))
        lagForeldelse(Testdata.behandling.id, periode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, periode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.utbetaltYtelsesbeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(r.riktigYtelsesbeløp).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `beregn skal beregne riktige beløp ved delvis feilutbetaling for perioder sammenslått til en logisk periode`() {
        val skatteprosent = BigDecimal.valueOf(10)
        val periode1 = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        val periode2 = Periode(LocalDate.of(2019, 5, 4), LocalDate.of(2019, 5, 6))
        val logiskPeriode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 6))
        val utbetalt1 = BigDecimal.valueOf(10000)
        val nyttBeløp1 = BigDecimal.valueOf(5000)
        val utbetalt2 = BigDecimal.valueOf(10000)
        val nyttBeløp2 = BigDecimal.valueOf(100)
        val feilutbetalt2 = utbetalt2.subtract(nyttBeløp2)
        val feilutbetalt1 = utbetalt1.subtract(nyttBeløp1)
        val grunnlagPeriode1: Kravgrunnlagsperiode432 =
                lagGrunnlagPeriode(periode1, 1000, setOf(lagYtelBeløp(utbetalt1, nyttBeløp1, skatteprosent),
                                                         lagFeilBeløp(feilutbetalt1)))
        val grunnlagPeriode2: Kravgrunnlagsperiode432 =
                lagGrunnlagPeriode(periode2, 1000, setOf(lagYtelBeløp(utbetalt2, nyttBeløp2, skatteprosent),
                                                         lagFeilBeløp(feilutbetalt2)))
        val grunnlag: Kravgrunnlag431 = lagGrunnlag(setOf(grunnlagPeriode1, grunnlagPeriode2))
        kravgrunnlagRepository.insert(grunnlag)
        lagForeldelse(Testdata.behandling.id, logiskPeriode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, logiskPeriode)
        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.periode).isEqualTo(logiskPeriode)
        assertThat(r.utbetaltYtelsesbeløp).isEqualByComparingTo(utbetalt1.add(utbetalt2))
        assertThat(r.riktigYtelsesbeløp).isEqualByComparingTo(nyttBeløp1.add(nyttBeløp2))
    }

    @Test
    fun `beregn skal beregne tilbakekrevingsbeløp for periode som ikke er foreldet medSkattProsent når beregnet periode er på tvers av grunnlag periode`() {
        val periode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        val periode1 = Periode(LocalDate.of(2019, 5, 4), LocalDate.of(2019, 5, 6))
        val logiskPeriode = Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 6))
        val grunnlagPeriode: Kravgrunnlagsperiode432 =
                lagGrunnlagPeriode(periode,
                                   1000,
                                   setOf(lagYtelBeløp(BigDecimal.valueOf(10000), BigDecimal.valueOf(10)),
                                         lagFeilBeløp(BigDecimal.valueOf(10000))))
        val grunnlagPeriode1: Kravgrunnlagsperiode432 =
                lagGrunnlagPeriode(periode1, 1000, setOf(lagYtelBeløp(BigDecimal.valueOf(10000), BigDecimal.valueOf(10)),
                                                         lagFeilBeløp(BigDecimal.valueOf(10000))))
        val grunnlag: Kravgrunnlag431 = lagGrunnlag(setOf(grunnlagPeriode, grunnlagPeriode1))
        kravgrunnlagRepository.insert(grunnlag)
        lagForeldelse(Testdata.behandling.id, logiskPeriode, Foreldelsesvurderingstype.IKKE_FORELDET, null)
        lagVilkårsvurderingMedForsett(Testdata.behandling.id, logiskPeriode)

        val beregningsresultat: Beregningsresultat = tilbakekrevingsberegningService.beregn(Testdata.behandling.id)
        val resultat: List<Beregningsresultatsperiode> = beregningsresultat.beregningsresultatsperioder
        assertThat(resultat).hasSize(1)
        val r: Beregningsresultatsperiode = resultat[0]
        assertThat(r.periode).isEqualTo(logiskPeriode)
        assertThat(r.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(22000))
        assertThat(r.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(r.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(r.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(20000))
        assertThat(r.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(r.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(r.skattebeløp).isEqualByComparingTo(BigDecimal.valueOf(2000))
        assertThat(r.tilbakekrevingsbeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(20000))
        assertThat(beregningsresultat.vedtaksresultat).isEqualByComparingTo(Vedtaksresultat.FULL_TILBAKEBETALING)
    }

    @Test
    fun `beregnBeløp skal beregne feilutbetaltBeløp når saksbehandler deler opp periode`() {
        val kravgrunnlag431 = Testdata.kravgrunnlag431
        val feilkravgrunnlagsbeløp = Testdata.feilKravgrunnlagsbeløp433
        val yteseskravgrunnlagsbeløp = Testdata.ytelKravgrunnlagsbeløp433
        val førsteKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
                .copy(periode = Periode(YearMonth.of(2017, 1), YearMonth.of(2017, 1)),
                      beløp = setOf(feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID())))
        val andreKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
                .copy(id = UUID.randomUUID(),
                      periode = Periode(YearMonth.of(2017, 2), YearMonth.of(2017, 2)),
                      beløp = setOf(feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID())))
        kravgrunnlagRepository.insert(kravgrunnlag431.copy(perioder = setOf(førsteKravgrunnlagsperiode,
                                                                            andreKravgrunnlagsperiode)))

        val beregnetPerioderDto = tilbakekrevingsberegningService.beregnBeløp(behandlingId = Testdata.behandling.id,
                                                                perioder = listOf(PeriodeDto(LocalDate.of(2017, 1, 1),
                                                                                             LocalDate.of(2017, 1, 31)),
                                                                                  PeriodeDto(LocalDate.of(2017, 2, 1),
                                                                                             LocalDate.of(2017, 2, 28))))
        assertEquals(2, beregnetPerioderDto.beregnetPerioder.size)
        assertEquals(PeriodeDto(LocalDate.of(2017, 1, 1),
                                LocalDate.of(2017, 1, 31)), beregnetPerioderDto.beregnetPerioder[0].periode)
        assertEquals(BigDecimal("10000"), beregnetPerioderDto.beregnetPerioder[0].feilutbetaltBeløp)
        assertEquals(PeriodeDto(LocalDate.of(2017, 2, 1),
                                LocalDate.of(2017, 2, 28)), beregnetPerioderDto.beregnetPerioder[1].periode)
        assertEquals(BigDecimal("10000"), beregnetPerioderDto.beregnetPerioder[1].feilutbetaltBeløp)
    }

    @Test
    fun `beregnBeløp skal ikke beregne feilutbetaltBeløp når saksbehandler deler opp periode som ikke starter første dato`() {
        val exception = assertFailsWith<RuntimeException> {
            tilbakekrevingsberegningService.beregnBeløp(behandlingId = Testdata.behandling.id,
                                          perioder = listOf(PeriodeDto(LocalDate.of(2017, 1, 1),
                                                                       LocalDate.of(2017, 1, 31)),
                                                            PeriodeDto(LocalDate.of(2017, 2, 16),
                                                                       LocalDate.of(2017, 2, 28))))
        }
        assertEquals("Periode med ${
            PeriodeDto(LocalDate.of(2017, 2, 16),
                       LocalDate.of(2017, 2, 28))
        } er ikke i hele måneder", exception.message)
    }

    @Test
    fun `beregnBeløp skal ikke beregne feilutbetaltBeløp når saksbehandler deler opp periode som ikke slutter siste dato`() {
        val exception = assertFailsWith<RuntimeException> {
            tilbakekrevingsberegningService.beregnBeløp(behandlingId = Testdata.behandling.id,
                                          perioder = listOf(PeriodeDto(LocalDate.of(2017, 1, 1),
                                                                       LocalDate.of(2017, 1, 27)),
                                                            PeriodeDto(LocalDate.of(2017, 2, 1),
                                                                       LocalDate.of(2017, 2, 28))))
        }
        assertEquals("Periode med ${
            PeriodeDto(LocalDate.of(2017, 1, 1),
                       LocalDate.of(2017, 1, 27))
        } er ikke i hele måneder", exception.message)
    }

    private fun lagVilkårsvurderingMedForsett(behandlingId: UUID, vararg perioder: Periode) {
        val vurderingsperioder = perioder.map {
            Vilkårsvurderingsperiode(periode = Periode(it.fom, it.tom),
                                     begrunnelse = "foo",
                                     vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                                     aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                           begrunnelse = "foo"))
        }.toSet()
        val vurdering = Vilkårsvurdering(behandlingId = behandlingId,
                                         perioder = vurderingsperioder)

        vilkårsvurderingRepository.insert(vurdering)
    }

    private fun lagForeldelse(behandlingId: UUID,
                              periode: Periode,
                              resultat: Foreldelsesvurderingstype,
                              foreldelsesFrist: LocalDate?) {
        val vurdertForeldelse =
                VurdertForeldelse(behandlingId = behandlingId,
                                  foreldelsesperioder = setOf(Foreldelsesperiode(periode = periode,
                                                                                 begrunnelse = "foo",
                                                                                 foreldelsesvurderingstype = resultat,
                                                                                 foreldelsesfrist = foreldelsesFrist)))
        vurdertForeldelseRepository.insert(vurdertForeldelse)
    }

    private fun lagKravgrunnlag(periode: Periode, skattProsent: BigDecimal) {
        val p = Testdata.kravgrunnlagsperiode432.copy(id = UUID.randomUUID(),
                                                      periode = periode,
                                                      beløp = setOf(lagFeilBeløp(BigDecimal.valueOf(10000)),
                                                                    lagYtelBeløp(BigDecimal.valueOf(10000), skattProsent)))
        val grunnlag: Kravgrunnlag431 = Testdata.kravgrunnlag431.copy(perioder = setOf(p))
        kravgrunnlagRepository.insert(grunnlag)
    }

    private fun lagFeilBeløp(feilutbetaling: BigDecimal): Kravgrunnlagsbeløp433 {
        return Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID(),
                                                       nyttBeløp = feilutbetaling)

    }

    private fun lagYtelBeløp(utbetalt: BigDecimal, skatteprosent: BigDecimal): Kravgrunnlagsbeløp433 {
        return Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID(),
                                                       opprinneligUtbetalingsbeløp = utbetalt,
                                                       nyttBeløp = BigDecimal.ZERO,
                                                       skatteprosent = skatteprosent)
    }

    private fun lagYtelBeløp(utbetalt: BigDecimal,
                             nyttBeløp: BigDecimal,
                             skatteprosent: BigDecimal): Kravgrunnlagsbeløp433 {
        return Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID(),
                                                       opprinneligUtbetalingsbeløp = utbetalt,
                                                       nyttBeløp = nyttBeløp,
                                                       skatteprosent = skatteprosent)
    }

    private fun lagGrunnlagPeriode(periode: Periode,
                                   skattMnd: Int,
                                   beløp: Set<Kravgrunnlagsbeløp433> = setOf()): Kravgrunnlagsperiode432 {
        return Kravgrunnlagsperiode432(periode = periode,
                                       månedligSkattebeløp = BigDecimal.valueOf(skattMnd.toLong()),
                                       beløp = beløp)

    }

    private fun lagGrunnlag(perioder: Set<Kravgrunnlagsperiode432>): Kravgrunnlag431 {
        return Testdata.kravgrunnlag431.copy(perioder = perioder)

    }
}
