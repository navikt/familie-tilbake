package no.nav.familie.tilbake.beregning

import com.google.common.collect.Lists
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TilbakekrevingsberegningVilkårTest {

    private lateinit var vurdering: Vilkårsvurderingsperiode
    private lateinit var grunnlagsperiodeMedSkatteprosent: GrunnlagsperiodeMedSkatteprosent
    private lateinit var forstoBurdeForstattVurdering: Vilkårsvurderingsperiode

    @BeforeEach
    fun setup() {
        vurdering =
                Vilkårsvurderingsperiode(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                                         periode = Periode(LocalDate.of(2019, 5, 1),
                                                           LocalDate.of(2019, 5, 3)),
                                         begrunnelse = "foo")
        forstoBurdeForstattVurdering =
                Vilkårsvurderingsperiode(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                                         periode = Periode(LocalDate.of(2019, 5, 1),
                                                           LocalDate.of(2019, 5, 3)),
                                         begrunnelse = "foo")

        grunnlagsperiodeMedSkatteprosent =
                GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.ZERO)
    }

    @Test
    fun `beregn skalkreve tilbake alt med renter ved forsett og illeggRenter ikke satt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                         begrunnelse = "foo"))


        //act
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)

        //assert
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.tilbakekrevingsbeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.rentebeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
    }

    @Test
    fun `beregn skalkreve tilbake alt med renter ved forsett og illeggRenter satt true`() {
        forstoBurdeForstattVurdering =
                forstoBurdeForstattVurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                                        begrunnelse = "foo",
                                                                                        ileggRenter = true))

        //act
        val resultat: Beregningsresultatsperiode = beregn(forstoBurdeForstattVurdering,
                                                          BigDecimal.valueOf(10000),
                                                          Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                                                          true)

        //assert
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.tilbakekrevingsbeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.rentebeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
    }

    @Test
    fun `beregn skalkreve tilbake alt uten renter ved forsett og illeggRenter satt false`() {
        forstoBurdeForstattVurdering =
                forstoBurdeForstattVurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                                        begrunnelse = "foo",
                                                                                        ileggRenter = false))

        //act
        val resultat: Beregningsresultatsperiode = beregn(forstoBurdeForstattVurdering,
                                                          BigDecimal.valueOf(10000),
                                                          Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                                                          true)

        //assert
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.tilbakekrevingsbeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.rentebeløp).isEqualByComparingTo(BigDecimal.valueOf(0))
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
    }

    @Test
    fun `beregn skalkreve tilbake alt ved grov uaktsomhet når ikke annet er valgt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = false,
                                                                         ileggRenter = true))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.GROV_UAKTSOMHET)
    }

    @Test
    fun `beregn skalikke kreve noe når sjette ledd benyttes for å_ikke gjøre innkreving av småbeløp`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = false,
                                                                         tilbakekrevSmåbeløp = false))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(522), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.SIMPEL_UAKTSOMHET)
    }

    @Test
    fun `beregn skalkreve tilbake deler ved grov uaktsomhet når særlige grunner er valgt og ilegge renter når det er valgt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = true,
                                                                         andelTilbakekreves = BigDecimal.valueOf(70)))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(7700))
        assertThat(resultat.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
    }

    @Test
    fun `beregn skal kreve tilbake deler ved grov uaktsomhet ved når særlige grunner og ikke ilegge renter når det er false`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = false,
                                                                         andelTilbakekreves = BigDecimal.valueOf(70)))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(7000))
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.rentebeløp).isZero()
    }

    @Test
    fun `beregn skaltakle desimaler på prosenter som tilbakekreves`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = false,
                                                                         andelTilbakekreves = BigDecimal("0.01")))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(70000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(7))
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.rentebeløp).isZero()
    }

    @Test
    fun `beregn skalkreve tilbake manuelt beløp når det er satt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = false,
                                                                         manueltSattBeløp = BigDecimal.valueOf(6556)))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(6556))
        assertThat(resultat.renteprosent).isNull()
    }

    @Test
    fun `beregn skalkreve tilbake manuelt beløp med renter når det er satt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = true,
                                                                         manueltSattBeløp = BigDecimal.valueOf(6000)))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(6600))
        assertThat(resultat.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
    }

    @Test
    fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro`() {
        vurdering = vurdering.copy(godTro = VilkårsvurderingGodTro(beløpErIBehold = true,
                                                                   beløpTilbakekreves = BigDecimal.valueOf(8991),
                                                                   begrunnelse = "foo"))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.andelAvBeløp).isNull()
        assertThat(resultat.vurdering).isEqualTo(AnnenVurdering.GOD_TRO)
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
    }

    @Test
    fun `beregn skalkreve tilbake ingenting når det er god tro og beløp ikke er i_behold`() {
        vurdering = vurdering.copy(godTro = VilkårsvurderingGodTro(beløpErIBehold = false,
                                                                   begrunnelse = "foo"))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.andelAvBeløp).isZero()
        assertThat(resultat.vurdering).isEqualTo(AnnenVurdering.GOD_TRO)
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
    }

    @Test
    fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro med skatt prosent`() {
        vurdering = vurdering.copy(godTro = VilkårsvurderingGodTro(beløpErIBehold = true,
                                                                   beløpTilbakekreves = BigDecimal.valueOf(8991),
                                                                   begrunnelse = "foo"))
        val grunnlagPeriodeMedSkattProsent =
                GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        //assert
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.andelAvBeløp).isNull()
        assertThat(resultat.vurdering).isEqualTo(AnnenVurdering.GOD_TRO)
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
        assertThat(resultat.skattebeløp).isEqualByComparingTo(BigDecimal.valueOf(899))
        assertThat(resultat.tilbakekrevingsbeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(8092))
    }

    @Test
    fun `beregn skalkreve tilbake alt med renter ved forsett med skatt prosent`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                         begrunnelse = "foo"))
        val grunnlagPeriodeMedSkattProsent =
                GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        //act
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)

        //assert
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.tilbakekrevingsbeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.rentebeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.renteprosent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(resultat.skattebeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.tilbakekrevingsbeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(10000))
    }

    @Test
    fun `beregn skalkreve tilbake alt uten renter ved forsett men frisinn med skatt prosent`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                         begrunnelse = "foo"))
        val grunnlagPeriodeMedSkattProsent =
                GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        //act
        val resultat: Beregningsresultatsperiode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), false)

        //assert
        assertThat(resultat.tilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.tilbakekrevingsbeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.rentebeløp).isEqualByComparingTo(BigDecimal.valueOf(0))
        assertThat(resultat.renteprosent).isNull()
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(resultat.skattebeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.tilbakekrevingsbeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(9000))
    }

    fun beregn(vilkårVurdering: Vilkårsvurderingsperiode,
               feilutbetalt: BigDecimal,
               perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
               beregnRenter: Boolean): Beregningsresultatsperiode {
        val delresultat = FordeltKravgrunnlagsbeløp(feilutbetalt, feilutbetalt, BigDecimal.ZERO)
        return TilbakekrevingsberegningVilkår.beregn(vilkårVurdering, delresultat, perioderMedSkatteprosent, beregnRenter)
    }
}
