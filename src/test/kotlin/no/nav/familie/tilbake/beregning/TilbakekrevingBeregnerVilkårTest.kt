package no.nav.familie.tilbake.beregning

import com.google.common.collect.Lists
import no.nav.familie.tilbake.beregning.modell.BeregningResultatPeriode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagBeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagPeriodeMedSkattProsent
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Aktsomhet
import no.nav.familie.tilbake.domain.tbd.AnnenVurdering
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingGodTro
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TilbakekrevingBeregnerVilkårTest {

    private lateinit var vurdering: Vilkårsvurderingsperiode
    private lateinit var grunnlagPeriodeMedSkattProsent: GrunnlagPeriodeMedSkattProsent
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

        grunnlagPeriodeMedSkattProsent =
                GrunnlagPeriodeMedSkattProsent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.ZERO)
    }

    @Test
    fun `beregn skalkreve tilbake alt med renter ved forsett og illeggRenter ikke satt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                         begrunnelse = "foo"))


        //act
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)

        //assert
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.tilbakekrevingBeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.renteBeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.renterProsent).isEqualByComparingTo(BigDecimal.valueOf(10))
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
        val resultat: BeregningResultatPeriode = beregn(forstoBurdeForstattVurdering,
                                                        BigDecimal.valueOf(10000),
                                                        Lists.newArrayList(grunnlagPeriodeMedSkattProsent),
                                                        true)

        //assert
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.tilbakekrevingBeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.renteBeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.renterProsent).isEqualByComparingTo(BigDecimal.valueOf(10))
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
        val resultat: BeregningResultatPeriode = beregn(forstoBurdeForstattVurdering,
                                                        BigDecimal.valueOf(10000),
                                                        Lists.newArrayList(grunnlagPeriodeMedSkattProsent),
                                                        true)

        //assert
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.tilbakekrevingBeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.renteBeløp).isEqualByComparingTo(BigDecimal.valueOf(0))
        assertThat(resultat.renterProsent).isNull()
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
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.renterProsent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.GROV_UAKTSOMHET)
    }

    @Test
    fun `beregn skalikke kreve noe når sjette ledd benyttes for å_ikke gjøre innkreving av småbeløp`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = false,
                                                                         tilbakekrevSmåbeløp = false))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(522), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.ZERO)
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
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(7700))
        assertThat(resultat.renterProsent).isEqualByComparingTo(BigDecimal.valueOf(10))
    }

    @Test
    fun `beregn skal kreve tilbake deler ved grov uaktsomhet ved når særlige grunner og ikke ilegge renter når det er false`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = false,
                                                                         andelTilbakekreves = BigDecimal.valueOf(70)))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(7000))
        assertThat(resultat.renterProsent).isNull()
        assertThat(resultat.renteBeløp).isZero()
    }

    @Test
    fun `beregn skaltakle desimaler på prosenter som tilbakekreves`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = false,
                                                                         andelTilbakekreves = BigDecimal("0.01")))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(70000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(7))
        assertThat(resultat.renterProsent).isNull()
        assertThat(resultat.renteBeløp).isZero()
    }

    @Test
    fun `beregn skalkreve tilbake manuelt beløp når det er satt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = false,
                                                                         manueltSattBeløp = BigDecimal.valueOf(6556)))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(6556))
        assertThat(resultat.renterProsent).isNull()
    }

    @Test
    fun `beregn skalkreve tilbake manuelt beløp med renter når det er satt`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                                         begrunnelse = "foo",
                                                                         særligeGrunnerTilReduksjon = true,
                                                                         ileggRenter = true,
                                                                         manueltSattBeløp = BigDecimal.valueOf(6000)))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(6600))
        assertThat(resultat.renterProsent).isEqualByComparingTo(BigDecimal.valueOf(10))
    }

    @Test
    fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro`() {
        vurdering = vurdering.copy(godTro = VilkårsvurderingGodTro(beløpErIBehold = true,
                                                                   beløpTilbakekreves = BigDecimal.valueOf(8991),
                                                                   begrunnelse = "foo"))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
        assertThat(resultat.renterProsent).isNull()
        assertThat(resultat.andelAvBeløp).isNull()
        assertThat(resultat.vurdering).isEqualTo(AnnenVurdering.GOD_TRO)
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
    }

    @Test
    fun `beregn skalkreve tilbake ingenting når det er god tro og beløp ikke er i_behold`() {
        vurdering = vurdering.copy(godTro = VilkårsvurderingGodTro(beløpErIBehold = false,
                                                                   begrunnelse = "foo"))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(resultat.renterProsent).isNull()
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
                GrunnlagPeriodeMedSkattProsent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        //assert
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
        assertThat(resultat.renterProsent).isNull()
        assertThat(resultat.andelAvBeløp).isNull()
        assertThat(resultat.vurdering).isEqualTo(AnnenVurdering.GOD_TRO)
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isEqualByComparingTo(BigDecimal.valueOf(8991))
        assertThat(resultat.skattBeløp).isEqualByComparingTo(BigDecimal.valueOf(899))
        assertThat(resultat.tilbakekrevingBeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(8092))
    }

    @Test
    fun `beregn skalkreve tilbake alt med renter ved forsett med skatt prosent`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                         begrunnelse = "foo"))
        val grunnlagPeriodeMedSkattProsent =
                GrunnlagPeriodeMedSkattProsent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        //act
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)

        //assert
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(11000))
        assertThat(resultat.tilbakekrevingBeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.renteBeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.renterProsent).isEqualByComparingTo(BigDecimal.valueOf(10))
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(resultat.skattBeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.tilbakekrevingBeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(10000))
    }

    @Test
    fun `beregn skalkreve tilbake alt uten renter ved forsett men frisinn med skatt prosent`() {
        vurdering = vurdering.copy(aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT,
                                                                         begrunnelse = "foo"))
        val grunnlagPeriodeMedSkattProsent =
                GrunnlagPeriodeMedSkattProsent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        //act
        val resultat: BeregningResultatPeriode =
                beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), false)

        //assert
        assertThat(resultat.tilbakekrevingBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.tilbakekrevingBeløpUtenRenter).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.renteBeløp).isEqualByComparingTo(BigDecimal.valueOf(0))
        assertThat(resultat.renterProsent).isNull()
        assertThat(resultat.andelAvBeløp).isEqualByComparingTo(BigDecimal.valueOf(100))
        assertThat(resultat.feilutbetaltBeløp).isEqualByComparingTo(BigDecimal.valueOf(10000))
        assertThat(resultat.vurdering).isEqualTo(Aktsomhet.FORSETT)
        assertThat(resultat.periode).isEqualTo(Periode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3)))
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isNull()
        assertThat(resultat.skattBeløp).isEqualByComparingTo(BigDecimal.valueOf(1000))
        assertThat(resultat.tilbakekrevingBeløpEtterSkatt).isEqualByComparingTo(BigDecimal.valueOf(9000))
    }

    fun beregn(vilkårVurdering: Vilkårsvurderingsperiode,
               feilutbetalt: BigDecimal,
               perioderMedSkattProsent: List<GrunnlagPeriodeMedSkattProsent>,
               beregnRenter: Boolean): BeregningResultatPeriode {
        val delresultat = FordeltKravgrunnlagBeløp(feilutbetalt, feilutbetalt, BigDecimal.ZERO)
        return TilbakekrevingBeregnerVilkår.beregn(vilkårVurdering, delresultat, perioderMedSkattProsent, beregnRenter)
    }
}