package no.nav.familie.tilbake.beregning

import com.google.common.collect.Lists
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
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TilbakekrevingsberegningVilkårTest {

    private var vilkårsvurderingsperiode = Vilkårsvurderingsperiode(
        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        periode = Månedsperiode(
            LocalDate.of(2019, 5, 1),
            LocalDate.of(2019, 5, 3)
        ),
        begrunnelse = "foo"
    )

    private lateinit var grunnlagsperiodeMedSkatteprosent: GrunnlagsperiodeMedSkatteprosent
    private lateinit var forstoBurdeForstattVurdering: Vilkårsvurderingsperiode

    private val FEILUTBETALT_BELØP = BigDecimal.valueOf(10_000)
    private val RENTEPROSENT = BigDecimal.valueOf(10)

    @BeforeEach
    fun setup() {
        vilkårsvurderingsperiode =
            Vilkårsvurderingsperiode(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                periode = Månedsperiode(
                    LocalDate.of(2019, 5, 1),
                    LocalDate.of(2019, 5, 3)
                ),
                begrunnelse = "foo"
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
            GrunnlagsperiodeMedSkatteprosent(vilkårsvurderingsperiode.periode, BigDecimal.valueOf(10000), BigDecimal.ZERO)
    }

    @Nested
    inner class VilkårsvurderingGodTro {
        @Test
        fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro`() {
            val manueltBeløp = BigDecimal.valueOf(8991)

            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                godTro = VilkårsvurderingGodTro(
                    beløpErIBehold = true,
                    beløpTilbakekreves = manueltBeløp,
                    begrunnelse = "foo"
                )
            )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = AnnenVurdering.GOD_TRO,
                manueltSattTilbakekrevingsbeløp = manueltBeløp,
                tilbakekrevingsbeløpUtenRenter = manueltBeløp,
                tilbakekrevingsbeløp = manueltBeløp,
                andelAvBeløp = null
            )
        }

        @Test
        fun `beregn skalkreve tilbake ingenting når det er god tro og beløp ikke er i_behold`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                godTro = VilkårsvurderingGodTro(
                    beløpErIBehold = false,
                    begrunnelse = "foo"
                )
            )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = AnnenVurdering.GOD_TRO,
                tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
                tilbakekrevingsbeløp = BigDecimal.ZERO,
                andelAvBeløp = BigDecimal.ZERO
            )
        }

        @Test
        fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro med skatt prosent`() {
            val beløpTilbakekreves = BigDecimal.valueOf(8991)
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                godTro = VilkårsvurderingGodTro(
                    beløpErIBehold = true,
                    beløpTilbakekreves = beløpTilbakekreves,
                    begrunnelse = "foo"
                )
            )
            val grunnlagPeriodeMedSkattProsent =
                GrunnlagsperiodeMedSkatteprosent(
                    periode = vilkårsvurderingsperiode.periode,
                    tilbakekrevingsbeløp = FEILUTBETALT_BELØP,
                    skatteprosent = BigDecimal.valueOf(10)
                )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagPeriodeMedSkattProsent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = AnnenVurdering.GOD_TRO,
                tilbakekrevingsbeløpUtenRenter = beløpTilbakekreves,
                tilbakekrevingsbeløp = beløpTilbakekreves,
                manueltSattTilbakekrevingsbeløp = beløpTilbakekreves,
                skattebeløp = BigDecimal.valueOf(899),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(8092),
                andelAvBeløp = null
            )
        }
    }

    @Nested
    inner class VilkårsvurderingAktsomhetForsett {
        @Test
        fun `beregn skal kreve tilbake alt med renter ved forsett og illeggRenter ikke satt`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.FORSETT,
                    begrunnelse = "foo"
                )
            )

            // act
            val resultat: Beregningsresultatsperiode =
                beregn(vilkårVurdering = vilkårsvurderingsperiode, feilutbetalt = FEILUTBETALT_BELØP, perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent), beregnRenter = true)

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.FORSETT,
                tilbakekrevingsbeløpUtenRenter = FEILUTBETALT_BELØP,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                rentebeløp = BigDecimal.valueOf(1000),
                renteprosent = RENTEPROSENT,
                andelAvBeløp = BigDecimal.valueOf(100)
            )
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
                vilkårVurdering = forstoBurdeForstattVurdering,
                feilutbetalt = FEILUTBETALT_BELØP,
                perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                beregnRenter = true
            )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.FORSETT,
                vurderingsperiode = forstoBurdeForstattVurdering.periode,
                tilbakekrevingsbeløpUtenRenter = FEILUTBETALT_BELØP,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                rentebeløp = BigDecimal.valueOf(1000),
                renteprosent = RENTEPROSENT,
                andelAvBeløp = BigDecimal.valueOf(100)
            )
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
                vilkårVurdering = forstoBurdeForstattVurdering,
                feilutbetalt = FEILUTBETALT_BELØP,
                perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                beregnRenter = true
            )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.FORSETT,
                vurderingsperiode = forstoBurdeForstattVurdering.periode,
                tilbakekrevingsbeløpUtenRenter = FEILUTBETALT_BELØP,
                tilbakekrevingsbeløp = FEILUTBETALT_BELØP
            )
        }

        @Test
        fun `beregn skalkreve tilbake alt med renter ved forsett med skatt prosent`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.FORSETT,
                    begrunnelse = "foo"
                )
            )
            val grunnlagPeriodeMedSkattProsent =
                GrunnlagsperiodeMedSkatteprosent(
                    periode = vilkårsvurderingsperiode.periode,
                    tilbakekrevingsbeløp = FEILUTBETALT_BELØP,
                    skatteprosent = BigDecimal.valueOf(10)
                )

            // act
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagPeriodeMedSkattProsent),
                    beregnRenter = true
                )

            // assert
            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.FORSETT,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                renteprosent = RENTEPROSENT,
                rentebeløp = BigDecimal.valueOf(1000),
                skattebeløp = BigDecimal.valueOf(1000),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(10000)
            )
        }

        @Test
        fun `beregn skalkreve tilbake alt uten renter ved forsett men frisinn med skatt prosent`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.FORSETT,
                    begrunnelse = "foo"
                )
            )
            val grunnlagPeriodeMedSkattProsent =
                GrunnlagsperiodeMedSkatteprosent(
                    periode = vilkårsvurderingsperiode.periode,
                    tilbakekrevingsbeløp = FEILUTBETALT_BELØP,
                    skatteprosent = BigDecimal.valueOf(10)
                )

            // act
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagPeriodeMedSkattProsent),
                    beregnRenter = false
                )

            // assert
            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.FORSETT,
                skattebeløp = BigDecimal.valueOf(1000),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(9000)
            )
        }
    }

    @Nested
    inner class VilkårsvurderingAktsomhetGrovUaktsomhet {
        @Test
        fun `beregn skalkreve tilbake alt ved grov uaktsomhet når ikke annet er valgt`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    begrunnelse = "foo",
                    særligeGrunnerTilReduksjon = false,
                    ileggRenter = true
                )
            )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                renteprosent = RENTEPROSENT,
                rentebeløp = BigDecimal.valueOf(1000)
            )
        }

        @Test
        fun `beregn skalkreve tilbake deler ved grov uaktsomhet når særlige grunner er valgt og ilegge renter når det er valgt`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
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
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                andelAvBeløp = BigDecimal.valueOf(70),
                tilbakekrevingsbeløpUtenRenter = BigDecimal.valueOf(7000),
                tilbakekrevingsbeløp = BigDecimal.valueOf(7700),
                renteprosent = RENTEPROSENT,
                rentebeløp = BigDecimal.valueOf(700)
            )
        }

        @Test
        fun `beregn skal kreve tilbake deler ved grov uaktsomhet ved når særlige grunner og ikke ilegge renter når det er false`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
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
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                andelAvBeløp = BigDecimal.valueOf(70),
                tilbakekrevingsbeløpUtenRenter = BigDecimal.valueOf(7000),
                tilbakekrevingsbeløp = BigDecimal.valueOf(7000)
            )
        }

        @Test
        fun `beregn skaltakle desimaler på prosenter som tilbakekreves`() {
            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
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
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                andelAvBeløp = BigDecimal("0.01"),
                tilbakekrevingsbeløpUtenRenter = BigDecimal.valueOf(1),
                tilbakekrevingsbeløp = BigDecimal.valueOf(1)
            )
        }

        @Test
        fun `beregn skalkreve tilbake manuelt beløp når det er satt`() {
            val manueltSattBeløp = BigDecimal.valueOf(6556)

            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    begrunnelse = "foo",
                    særligeGrunnerTilReduksjon = true,
                    ileggRenter = false,
                    manueltSattBeløp = manueltSattBeløp
                ),
                godTro = null
            )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                tilbakekrevingsbeløpUtenRenter = manueltSattBeløp,
                tilbakekrevingsbeløp = manueltSattBeløp,
                manueltSattTilbakekrevingsbeløp = manueltSattBeløp,
                andelAvBeløp = null
            )
        }

        @Test
        fun `beregn skalkreve tilbake manuelt beløp med renter når det er satt`() {
            val manueltSattBeløp = BigDecimal.valueOf(6000)

            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    begrunnelse = "foo",
                    særligeGrunnerTilReduksjon = true,
                    ileggRenter = true,
                    manueltSattBeløp = manueltSattBeløp
                )
            )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = FEILUTBETALT_BELØP,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                tilbakekrevingsbeløpUtenRenter = manueltSattBeløp,
                tilbakekrevingsbeløp = BigDecimal.valueOf(6600),
                manueltSattTilbakekrevingsbeløp = manueltSattBeløp,
                andelAvBeløp = null,
                renteprosent = BigDecimal.valueOf(10),
                rentebeløp = BigDecimal.valueOf(600)
            )
        }
    }

    @Nested
    inner class VilkårsvurderingAktsomhetSimpelUaktsomhet {
        @Test
        fun `beregn skalikke kreve noe når sjette ledd benyttes for å ikke gjøre innkreving av småbeløp`() {
            val feilutbetaltBeløp = BigDecimal.valueOf(522)

            vilkårsvurderingsperiode = vilkårsvurderingsperiode.copy(
                aktsomhet = VilkårsvurderingAktsomhet(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    begrunnelse = "foo",
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = false
                )
            )

            // assert
            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurderingsperiode,
                    feilutbetalt = feilutbetaltBeløp,
                    perioderMedSkatteprosent = Lists.newArrayList(grunnlagsperiodeMedSkatteprosent),
                    beregnRenter = true
                )

            validerResultat(
                resultat = resultat,
                vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                feilutbetalt = feilutbetaltBeløp,
                andelAvBeløp = BigDecimal.ZERO,
                tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
                tilbakekrevingsbeløp = BigDecimal.ZERO
            )
        }
    }

    private fun beregn(
        vilkårVurdering: Vilkårsvurderingsperiode,
        feilutbetalt: BigDecimal,
        perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
        beregnRenter: Boolean
    ): Beregningsresultatsperiode {
        val delresultat = FordeltKravgrunnlagsbeløp(feilutbetalt, feilutbetalt, BigDecimal.ZERO)
        return TilbakekrevingsberegningVilkår.beregn(vilkårVurdering, delresultat, perioderMedSkatteprosent, beregnRenter)
    }

    /**
     * Metode brukt for å sørge for at alle verdiene i resultatet sjekkes i alle tester.
     * Default verdiene tilsvarer full tilbakekreving (andel = 100%) av hele ytelsesbeløpet uten renter og skatt.
     */
    private fun validerResultat(
        resultat: Beregningsresultatsperiode,
        vurderingsperiode: Månedsperiode = vilkårsvurderingsperiode.periode,
        vurdering: Vurdering,
        feilutbetalt: BigDecimal = FEILUTBETALT_BELØP,
        andelAvBeløp: BigDecimal? = BigDecimal.valueOf(100),
        renteprosent: BigDecimal? = null,
        manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
        tilbakekrevingsbeløpUtenRenter: BigDecimal = feilutbetalt, // feilutbetalt?
        rentebeløp: BigDecimal = BigDecimal.ZERO, // Sjekk denne
        tilbakekrevingsbeløp: BigDecimal = tilbakekrevingsbeløpUtenRenter, // Med mindre annet er fyllt inn vil det ikke legges på renter
        skattebeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevingsbeløpEtterSkatt: BigDecimal = tilbakekrevingsbeløp, // Med mindre annet er fyllt inn vil det ikke tas hensyn til skatt
        utbetaltYtelsesbeløp: BigDecimal = FEILUTBETALT_BELØP,
        riktigYtelsesbeløp: BigDecimal = BigDecimal.ZERO
    ) {
        assertThat(resultat.periode).isEqualTo(vurderingsperiode)
        assertThat(resultat.vurdering).isEqualTo(vurdering)
        assertThat(resultat.feilutbetaltBeløp).isEqualTo(feilutbetalt)
        assertThat(resultat.andelAvBeløp).isEqualTo(andelAvBeløp)
        assertThat(resultat.renteprosent).isEqualTo(renteprosent)
        assertThat(resultat.manueltSattTilbakekrevingsbeløp).isEqualTo(manueltSattTilbakekrevingsbeløp)
        assertThat(resultat.tilbakekrevingsbeløpUtenRenter).isEqualTo(tilbakekrevingsbeløpUtenRenter)
        assertThat(resultat.rentebeløp).isEqualTo(rentebeløp)
        assertThat(resultat.tilbakekrevingsbeløp).isEqualTo(tilbakekrevingsbeløp)
        assertThat(resultat.skattebeløp).isEqualTo(skattebeløp)
        assertThat(resultat.tilbakekrevingsbeløpEtterSkatt).isEqualTo(tilbakekrevingsbeløpEtterSkatt)
        assertThat(resultat.utbetaltYtelsesbeløp).isEqualTo(utbetaltYtelsesbeløp) // Rått beløp, ikke justert for ev. trekk
        assertThat(resultat.riktigYtelsesbeløp).isEqualTo(riktigYtelsesbeløp)
    }
}
