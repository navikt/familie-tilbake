package no.nav.familie.tilbake.beregning

import com.google.common.collect.Lists
import io.kotest.matchers.bigdecimal.shouldBeZero
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
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
        vurdering = Vilkårsvurderingsperiode(
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            periode = Månedsperiode(
                LocalDate.of(2019, 5, 1),
                LocalDate.of(2019, 5, 3)
            ),
            begrunnelse = "foo",
            aktsomhet = VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.FORSETT, begrunnelse = "foo")
        )
        forstoBurdeForstattVurdering =
            Vilkårsvurderingsperiode(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                periode = Månedsperiode(
                    LocalDate.of(2019, 5, 1),
                    LocalDate.of(2019, 5, 3)
                ),
                begrunnelse = "foo"
            )
        grunnlagsperiodeMedSkatteprosent =
            GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.ZERO)
    }

    @Test
    fun `beregn skal kreve tilbake alt med renter ved forsett og illeggRenter ikke satt`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.FORSETT,
                begrunnelse = "foo"
            )
        )

        // act
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)

        // assert
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        resultat.tilbakekrevingsbeløpUtenRenter shouldBe BigDecimal.valueOf(10000)
        resultat.rentebeløp shouldBe BigDecimal.valueOf(1000)
        resultat.renteprosent shouldBe BigDecimal.valueOf(10)
        resultat.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        resultat.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        resultat.vurdering shouldBe Aktsomhet.FORSETT
        resultat.periode shouldBe Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        resultat.manueltSattTilbakekrevingsbeløp shouldBe null
    }

    @Test
    fun `beregn skal kreve tilbake alt med renter ved forsett og illeggRenter satt true`() {
        forstoBurdeForstattVurdering =
            forstoBurdeForstattVurdering.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.FORSETT,
                    begrunnelse = "foo",
                    ileggRenter = true
                )
            )

        // act
        val resultat: Beregningsresultatsperiode = beregn(
            forstoBurdeForstattVurdering,
            BigDecimal.valueOf(10000),
            Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
            true
        )

        // assert
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        resultat.tilbakekrevingsbeløpUtenRenter shouldBe BigDecimal.valueOf(10000)
        resultat.rentebeløp shouldBe BigDecimal.valueOf(1000)
        resultat.renteprosent shouldBe BigDecimal.valueOf(10)
        resultat.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        resultat.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        resultat.vurdering shouldBe Aktsomhet.FORSETT
        resultat.periode shouldBe Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        resultat.manueltSattTilbakekrevingsbeløp shouldBe null
    }

    @Test
    fun `beregn skalkreve tilbake alt uten renter ved forsett og illeggRenter satt false`() {
        forstoBurdeForstattVurdering =
            forstoBurdeForstattVurdering.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.FORSETT,
                    begrunnelse = "foo",
                    ileggRenter = false
                )
            )

        // act
        val resultat: Beregningsresultatsperiode = beregn(
            forstoBurdeForstattVurdering,
            BigDecimal.valueOf(10000),
            Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
            true
        )

        // assert
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(10000)
        resultat.tilbakekrevingsbeløpUtenRenter shouldBe BigDecimal.valueOf(10000)
        resultat.rentebeløp shouldBe BigDecimal.valueOf(0)
        resultat.renteprosent shouldBe null
        resultat.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        resultat.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        resultat.vurdering shouldBe Aktsomhet.FORSETT
        resultat.periode shouldBe Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        resultat.manueltSattTilbakekrevingsbeløp shouldBe null
    }

    @Test
    fun `beregn skalkreve tilbake alt ved grov uaktsomhet når ikke annet er valgt`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = false,
                ileggRenter = true
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        resultat.renteprosent shouldBe BigDecimal.valueOf(10)
        resultat.vurdering shouldBe Aktsomhet.GROV_UAKTSOMHET
    }

    @Test
    fun `beregn skalikke kreve noe når sjette ledd benyttes for å ikke gjøre innkreving av småbeløp`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = false,
                tilbakekrevSmåbeløp = false
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(522), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.ZERO
        resultat.vurdering shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
    }

    @Test
    fun `beregn skalkreve tilbake deler ved grov uaktsomhet når særlige grunner er valgt og ilegge renter når det er valgt`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = true,
                ileggRenter = true,
                andelTilbakekreves = BigDecimal.valueOf(70)
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(7700)
        resultat.renteprosent shouldBe BigDecimal.valueOf(10)
    }

    @Test
    fun `beregn skal kreve tilbake deler ved grov uaktsomhet ved når særlige grunner og ikke ilegge renter når det er false`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = true,
                ileggRenter = false,
                andelTilbakekreves = BigDecimal.valueOf(70)
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(7000)
        resultat.renteprosent shouldBe null
        resultat.rentebeløp.shouldBeZero()
    }

    @Test
    fun `beregn skaltakle desimaler på prosenter som tilbakekreves`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = true,
                ileggRenter = false,
                andelTilbakekreves = BigDecimal("0.01")
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(70000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(7)
        resultat.renteprosent shouldBe null
        resultat.rentebeløp.shouldBeZero()
    }

    @Test
    fun `beregn skalkreve tilbake manuelt beløp når det er satt`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = true,
                ileggRenter = false,
                manueltSattBeløp = BigDecimal.valueOf(6556)
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(6556)
        resultat.renteprosent shouldBe null
    }

    @Test
    fun `beregn skalkreve tilbake manuelt beløp med renter når det er satt`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                begrunnelse = "foo",
                særligeGrunnerTilReduksjon = true,
                ileggRenter = true,
                manueltSattBeløp = BigDecimal.valueOf(6000)
            )
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(6600)
        resultat.renteprosent shouldBe BigDecimal.valueOf(10)
    }

    @Test
    fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro`() {
        vurdering = vurdering.copy(
            godTro = VilkårsvurderingGodTro(
                beløpErIBehold = true,
                beløpTilbakekreves = BigDecimal.valueOf(8991),
                begrunnelse = "foo"
            ),
            aktsomhet = null
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(8991)
        resultat.renteprosent shouldBe null
        resultat.andelAvBeløp shouldBe null
        resultat.vurdering shouldBe AnnenVurdering.GOD_TRO
        resultat.manueltSattTilbakekrevingsbeløp shouldBe BigDecimal.valueOf(8991)
    }

    @Test
    fun `beregn skalkreve tilbake ingenting når det er god tro og beløp ikke er i_behold`() {
        vurdering = vurdering.copy(
            godTro = VilkårsvurderingGodTro(
                beløpErIBehold = false,
                begrunnelse = "foo"
            ),
            aktsomhet = null
        )

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.ZERO
        resultat.renteprosent shouldBe null
        resultat.andelAvBeløp!!.shouldBeZero()
        resultat.vurdering shouldBe AnnenVurdering.GOD_TRO
        resultat.manueltSattTilbakekrevingsbeløp shouldBe null
    }

    @Test
    fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro med skatt prosent`() {
        vurdering = vurdering.copy(
            godTro = VilkårsvurderingGodTro(
                beløpErIBehold = true,
                beløpTilbakekreves = BigDecimal.valueOf(8991),
                begrunnelse = "foo"
            ),
            aktsomhet = null
        )
        val grunnlagPeriodeMedSkattProsent =
            GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        // assert
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(8991)
        resultat.renteprosent shouldBe null
        resultat.andelAvBeløp shouldBe null
        resultat.vurdering shouldBe AnnenVurdering.GOD_TRO
        resultat.manueltSattTilbakekrevingsbeløp shouldBe BigDecimal.valueOf(8991)
        resultat.skattebeløp shouldBe BigDecimal.valueOf(899)
        resultat.tilbakekrevingsbeløpEtterSkatt shouldBe BigDecimal.valueOf(8092)
    }

    @Test
    fun `beregn skatt med 4 og 6 desimaler`() {
        val tilbakekrevingsbeløp = BigDecimal.valueOf(4212)
        val skatteprosent = BigDecimal.valueOf(33.9981)
        val grunnlagPeriodeMedSkattProsent =
            listOf(
                GrunnlagsperiodeMedSkatteprosent(
                    periode = vurdering.periode,
                    tilbakekrevingsbeløp = tilbakekrevingsbeløp,
                    skatteprosent = skatteprosent
                )
            )

        val resultat = beregn(
            vilkårVurdering = vurdering,
            feilutbetalt = tilbakekrevingsbeløp,
            perioderMedSkatteprosent = grunnlagPeriodeMedSkattProsent,
            beregnRenter = true,
            bruk6desimalerISkatteberegning = false
        )
        val resultatMed6Desimaler = beregn(
            vilkårVurdering = vurdering,
            feilutbetalt = tilbakekrevingsbeløp,
            perioderMedSkatteprosent = grunnlagPeriodeMedSkattProsent,
            beregnRenter = true,
            bruk6desimalerISkatteberegning = true
        )

        resultat.skattebeløp shouldBe BigDecimal.valueOf(1432)
        resultatMed6Desimaler.skattebeløp shouldBe BigDecimal.valueOf(1431)
    }

    @Test
    fun `beregn skalkreve tilbake alt med renter ved forsett med skatt prosent`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.FORSETT,
                begrunnelse = "foo"
            )
        )
        val grunnlagPeriodeMedSkattProsent =
            GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        // act
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), true)

        // assert
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(11000)
        resultat.tilbakekrevingsbeløpUtenRenter shouldBe BigDecimal.valueOf(10000)
        resultat.rentebeløp shouldBe BigDecimal.valueOf(1000)
        resultat.renteprosent shouldBe BigDecimal.valueOf(10)
        resultat.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        resultat.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        resultat.vurdering shouldBe Aktsomhet.FORSETT
        resultat.periode shouldBe Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        resultat.manueltSattTilbakekrevingsbeløp shouldBe null
        resultat.skattebeløp shouldBe BigDecimal.valueOf(1000)
        resultat.tilbakekrevingsbeløpEtterSkatt shouldBe BigDecimal.valueOf(10000)
    }

    @Test
    fun `beregn skalkreve tilbake alt uten renter ved forsett men frisinn med skatt prosent`() {
        vurdering = vurdering.copy(
            aktsomhet = VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.FORSETT,
                begrunnelse = "foo"
            )
        )
        val grunnlagPeriodeMedSkattProsent =
            GrunnlagsperiodeMedSkatteprosent(vurdering.periode, BigDecimal.valueOf(10000), BigDecimal.valueOf(10))

        // act
        val resultat: Beregningsresultatsperiode =
            beregn(vurdering, BigDecimal.valueOf(10000), Lists.newArrayList(grunnlagPeriodeMedSkattProsent), false)

        // assert
        resultat.tilbakekrevingsbeløp shouldBe BigDecimal.valueOf(10000)
        resultat.tilbakekrevingsbeløpUtenRenter shouldBe BigDecimal.valueOf(10000)
        resultat.rentebeløp shouldBe BigDecimal.valueOf(0)
        resultat.renteprosent shouldBe null
        resultat.andelAvBeløp shouldBe BigDecimal.valueOf(100)
        resultat.feilutbetaltBeløp shouldBe BigDecimal.valueOf(10000)
        resultat.vurdering shouldBe Aktsomhet.FORSETT
        resultat.periode shouldBe Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 3))
        resultat.manueltSattTilbakekrevingsbeløp shouldBe null
        resultat.skattebeløp shouldBe BigDecimal.valueOf(1000)
        resultat.tilbakekrevingsbeløpEtterSkatt shouldBe BigDecimal.valueOf(9000)
    }

    private fun beregn(
        vilkårVurdering: Vilkårsvurderingsperiode,
        feilutbetalt: BigDecimal,
        perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
        beregnRenter: Boolean,
        bruk6desimalerISkatteberegning: Boolean = false
    ): Beregningsresultatsperiode {
        val delresultat = FordeltKravgrunnlagsbeløp(feilutbetalt, feilutbetalt, BigDecimal.ZERO)
        return TilbakekrevingsberegningVilkår.beregn(
            vilkårVurdering,
            delresultat,
            perioderMedSkatteprosent,
            beregnRenter,
            bruk6desimalerISkatteberegning
        )
    }
}
