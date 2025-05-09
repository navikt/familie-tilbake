package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class TilbakekrevingsberegningVilkårTest {
    private lateinit var vilkårsvurderingsperiode: Vilkårsvurderingsperiode
    private lateinit var forstodBurdeForståttVurdering: Vilkårsvurderingsperiode

    private val feilUtbetaltBeløp = BigDecimal.valueOf(10_000)
    private val renteProsent = BigDecimal.valueOf(10)

    @BeforeEach
    fun setup() {
        vilkårsvurderingsperiode =
            Vilkårsvurderingsperiode(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                periode =
                    Månedsperiode(
                        LocalDate.of(2019, 5, 1),
                        LocalDate.of(2019, 5, 3),
                    ),
                begrunnelse = "foo",
            )
        forstodBurdeForståttVurdering =
            Vilkårsvurderingsperiode(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                periode =
                    Månedsperiode(
                        LocalDate.of(2019, 5, 1),
                        LocalDate.of(2019, 5, 3),
                    ),
                begrunnelse = "foo",
            )
    }

    @Nested
    inner class VilkårsvurderingGodTro {
        @Test
        fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro`() {
            val manueltBeløp = BigDecimal.valueOf(8991)

            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    godTro =
                        VilkårsvurderingGodTro(
                            beløpErIBehold = true,
                            beløpTilbakekreves = manueltBeløp,
                            begrunnelse = "foo",
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = AnnenVurdering.GOD_TRO,
                manueltSattTilbakekrevingsbeløp = manueltBeløp,
                tilbakekrevingsbeløpUtenRenter = manueltBeløp,
                tilbakekrevingsbeløp = manueltBeløp,
                andelAvBeløp = null,
            )
        }

        @Test
        fun `beregn skalkreve tilbake ingenting når det er god tro og beløp ikke er i_behold`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    godTro =
                        VilkårsvurderingGodTro(
                            beløpErIBehold = false,
                            begrunnelse = "foo",
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = AnnenVurdering.GOD_TRO,
                tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
                tilbakekrevingsbeløp = BigDecimal.ZERO,
                andelAvBeløp = BigDecimal.ZERO,
            )
        }

        @Test
        fun `beregn skalkreve tilbake beløp som er i_behold uten renter ved god tro med skatt prosent`() {
            val beløpTilbakekreves = BigDecimal.valueOf(8991)
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    godTro =
                        VilkårsvurderingGodTro(
                            beløpErIBehold = true,
                            beløpTilbakekreves = beløpTilbakekreves,
                            begrunnelse = "foo",
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    skatteprosent = BigDecimal.valueOf(10),
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = AnnenVurdering.GOD_TRO,
                tilbakekrevingsbeløpUtenRenter = beløpTilbakekreves,
                tilbakekrevingsbeløp = beløpTilbakekreves,
                manueltSattTilbakekrevingsbeløp = beløpTilbakekreves,
                skattebeløp = BigDecimal.valueOf(899),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(8092),
                andelAvBeløp = null,
            )
        }
    }

    @Nested
    inner class VilkårsvurderingAktsomhetForsett {
        @Test
        fun `beregn skal kreve tilbake alt med renter ved forsett og illeggRenter ikke satt`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.FORSETT,
                            begrunnelse = "foo",
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.FORSETT,
                tilbakekrevingsbeløpUtenRenter = feilUtbetaltBeløp,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                rentebeløp = BigDecimal.valueOf(1000),
                renteprosent = renteProsent,
                andelAvBeløp = BigDecimal.valueOf(100),
            )
        }

        @Test
        fun `beregn skal kreve tilbake alt med renter ved forsett og illeggRenter satt true`() {
            val vilkårsvurdering =
                forstodBurdeForståttVurdering.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.FORSETT,
                            begrunnelse = "foo",
                            ileggRenter = true,
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.FORSETT,
                tilbakekrevingsbeløpUtenRenter = feilUtbetaltBeløp,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                rentebeløp = BigDecimal.valueOf(1000),
                renteprosent = renteProsent,
                andelAvBeløp = BigDecimal.valueOf(100),
            )
        }

        @Test
        fun `beregn skalkreve tilbake alt uten renter ved forsett og illeggRenter satt false`() {
            val vilkårsvurdering =
                forstodBurdeForståttVurdering.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.FORSETT,
                            begrunnelse = "foo",
                            ileggRenter = false,
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.FORSETT,
                tilbakekrevingsbeløpUtenRenter = feilUtbetaltBeløp,
                tilbakekrevingsbeløp = feilUtbetaltBeløp,
            )
        }

        @Test
        fun `beregn skalkreve tilbake alt med renter ved forsett med skatt prosent`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.FORSETT,
                            begrunnelse = "foo",
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    skatteprosent = BigDecimal.valueOf(10),
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.FORSETT,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                renteprosent = renteProsent,
                rentebeløp = BigDecimal.valueOf(1000),
                skattebeløp = BigDecimal.valueOf(1000),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(10000),
            )
        }

        @Test
        fun `beregn skalkreve tilbake alt uten renter ved forsett men frisinn med skatt prosent`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.FORSETT,
                            begrunnelse = "foo",
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    skatteprosent = BigDecimal.valueOf(10),
                    beregnRenter = false,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.FORSETT,
                skattebeløp = BigDecimal.valueOf(1000),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(9000),
            )
        }

        @Test
        fun `beregn skatt med feil ved 4 desimaler`() {
            val tilbakekrevingsbeløp = BigDecimal.valueOf(4212)
            val skatteprosent = BigDecimal.valueOf(33.9981)

            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.FORSETT,
                            begrunnelse = "foo",
                        ),
                )

            val resultat =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = tilbakekrevingsbeløp,
                    skatteprosent = skatteprosent,
                    tilbakekrevingsbeløp = tilbakekrevingsbeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.FORSETT,
                feilutbetalt = tilbakekrevingsbeløp,
                renteprosent = renteProsent,
                rentebeløp = BigDecimal.valueOf(421),
                tilbakekrevingsbeløpUtenRenter = tilbakekrevingsbeløp,
                tilbakekrevingsbeløp = BigDecimal.valueOf(4633),
                tilbakekrevingsbeløpEtterSkatt = BigDecimal.valueOf(3202),
                skattebeløp = BigDecimal.valueOf(1431),
            )
        }
    }

    @Nested
    inner class VilkårsvurderingAktsomhetGrovUaktsomhet {
        @Test
        fun `beregn skalkreve tilbake alt ved grov uaktsomhet når ikke annet er valgt`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            begrunnelse = "foo",
                            særligeGrunnerTilReduksjon = false,
                            ileggRenter = true,
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                tilbakekrevingsbeløp = BigDecimal.valueOf(11000),
                renteprosent = renteProsent,
                rentebeløp = BigDecimal.valueOf(1000),
            )
        }

        @Test
        fun `beregn skalkreve tilbake deler ved grov uaktsomhet når særlige grunner er valgt og ilegge renter når det er valgt`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            begrunnelse = "foo",
                            særligeGrunnerTilReduksjon = true,
                            ileggRenter = true,
                            andelTilbakekreves = BigDecimal.valueOf(70),
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                andelAvBeløp = BigDecimal.valueOf(70),
                tilbakekrevingsbeløpUtenRenter = BigDecimal.valueOf(7000),
                tilbakekrevingsbeløp = BigDecimal.valueOf(7700),
                renteprosent = renteProsent,
                rentebeløp = BigDecimal.valueOf(700),
            )
        }

        @Test
        fun `beregn skal kreve tilbake deler ved grov uaktsomhet ved når særlige grunner og ikke ilegge renter når det er false`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            begrunnelse = "foo",
                            særligeGrunnerTilReduksjon = true,
                            ileggRenter = false,
                            andelTilbakekreves = BigDecimal.valueOf(70),
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                andelAvBeløp = BigDecimal.valueOf(70),
                tilbakekrevingsbeløpUtenRenter = BigDecimal.valueOf(7000),
                tilbakekrevingsbeløp = BigDecimal.valueOf(7000),
            )
        }

        @Test
        fun `beregn skaltakle desimaler på prosenter som tilbakekreves`() {
            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            begrunnelse = "foo",
                            særligeGrunnerTilReduksjon = true,
                            ileggRenter = false,
                            andelTilbakekreves = BigDecimal("0.01"),
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                andelAvBeløp = BigDecimal("0.01"),
                tilbakekrevingsbeløpUtenRenter = BigDecimal.valueOf(1),
                tilbakekrevingsbeløp = BigDecimal.valueOf(1),
            )
        }

        @Test
        fun `beregn skalkreve tilbake manuelt beløp når det er satt`() {
            val manueltSattBeløp = BigDecimal.valueOf(6556)

            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            begrunnelse = "foo",
                            særligeGrunnerTilReduksjon = true,
                            ileggRenter = false,
                            manueltSattBeløp = manueltSattBeløp,
                        ),
                    godTro = null,
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilUtbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.GROV_UAKTSOMHET,
                tilbakekrevingsbeløpUtenRenter = manueltSattBeløp,
                tilbakekrevingsbeløp = manueltSattBeløp,
                manueltSattTilbakekrevingsbeløp = manueltSattBeløp,
                andelAvBeløp = null,
            )
        }
    }

    @Nested
    inner class VilkårsvurderingAktsomhetSimpelUaktsomhet {
        @Test
        fun `beregn skalikke kreve noe når sjette ledd benyttes for å ikke gjøre innkreving av småbeløp`() {
            val feilutbetaltBeløp = BigDecimal.valueOf(522)

            val vilkårsvurdering =
                vilkårsvurderingsperiode.copy(
                    aktsomhet =
                        VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                            begrunnelse = "foo",
                            særligeGrunnerTilReduksjon = false,
                            tilbakekrevSmåbeløp = false,
                        ),
                )

            val resultat: Beregningsresultatsperiode =
                beregn(
                    vilkårVurdering = vilkårsvurdering,
                    feilutbetalt = feilutbetaltBeløp,
                    beregnRenter = true,
                )

            resultat.skalHaVerdier(
                vilkårsvurdering = vilkårsvurdering,
                vurdering = Aktsomhet.SIMPEL_UAKTSOMHET,
                feilutbetalt = feilutbetaltBeløp,
                andelAvBeløp = BigDecimal.ZERO,
                tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
                tilbakekrevingsbeløp = BigDecimal.ZERO,
            )
        }
    }

    private fun beregn(
        vilkårVurdering: Vilkårsvurderingsperiode,
        feilutbetalt: BigDecimal,
        beregnRenter: Boolean,
        skatteprosent: BigDecimal = BigDecimal.ZERO,
        tilbakekrevingsbeløp: BigDecimal = BigDecimal.valueOf(10000),
    ): Beregningsresultatsperiode {
        return Vilkårsvurdert.opprett(
            VilkårsvurderingsperiodeAdapter(vurdering = vilkårVurdering),
            object : KravgrunnlagPeriodeAdapter {
                override fun periode(): Datoperiode = vilkårVurdering.periode.toDatoperiode()

                override fun feilutbetaltYtelsesbeløp(): BigDecimal = feilutbetalt

                override fun utbetaltYtelsesbeløp(): BigDecimal = feilutbetalt

                override fun riktigYteslesbeløp(): BigDecimal = BigDecimal.ZERO

                override fun beløpTilbakekreves(): List<KravgrunnlagPeriodeAdapter.BeløpTilbakekreves> = listOf(
                    object : KravgrunnlagPeriodeAdapter.BeløpTilbakekreves {
                        override fun beløp(): BigDecimal = tilbakekrevingsbeløp

                        override fun skatteprosent(): BigDecimal = skatteprosent
                    },
                )
            },
            beregnRenter,
            1,
        ).beregningsresultat()
    }

    /**
     * Metode brukt for å sørge for at alle verdiene i resultatet sjekkes i alle tester.
     * Default verdiene tilsvarer full tilbakekreving (andel = 100%) av hele ytelsesbeløpet uten renter og skatt.
     */
    private fun Beregningsresultatsperiode.skalHaVerdier(
        vilkårsvurdering: Vilkårsvurderingsperiode,
        vurdering: Vurdering,
        feilutbetalt: BigDecimal = feilUtbetaltBeløp,
        andelAvBeløp: BigDecimal? = BigDecimal.valueOf(100),
        renteprosent: BigDecimal? = null,
        manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
        tilbakekrevingsbeløpUtenRenter: BigDecimal = feilutbetalt,
        rentebeløp: BigDecimal = BigDecimal.ZERO,
        // Med mindre annet er fylt inn vil det ikke legges på renter
        tilbakekrevingsbeløp: BigDecimal = tilbakekrevingsbeløpUtenRenter,
        skattebeløp: BigDecimal = BigDecimal.ZERO,
        // Med mindre annet er fylt inn vil det ikke tas hensyn til skatt
        tilbakekrevingsbeløpEtterSkatt: BigDecimal = tilbakekrevingsbeløp,
        utbetaltYtelsesbeløp: BigDecimal = feilutbetalt,
        riktigYtelsesbeløp: BigDecimal = BigDecimal.ZERO,
    ) {
        assertThat(this.periode).isEqualTo(vilkårsvurdering.periode.toDatoperiode())
        assertThat(this.vurdering).isEqualTo(vurdering)
        assertThat(this.feilutbetaltBeløp.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(feilutbetalt.setScale(0))
        assertThat(this.andelAvBeløp?.setScale(2, RoundingMode.HALF_DOWN)).isEqualTo(andelAvBeløp?.setScale(2))
        assertThat(this.renteprosent?.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(renteprosent?.setScale(0))
        assertThat(this.manueltSattTilbakekrevingsbeløp?.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(manueltSattTilbakekrevingsbeløp?.setScale(0))
        assertThat(this.tilbakekrevingsbeløpUtenRenter.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(tilbakekrevingsbeløpUtenRenter.setScale(0))
        assertThat(this.rentebeløp.setScale(0, RoundingMode.DOWN)).isEqualTo(rentebeløp.setScale(0))
        assertThat(this.tilbakekrevingsbeløp.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(tilbakekrevingsbeløp.setScale(0))
        assertThat(this.skattebeløp.setScale(0, RoundingMode.DOWN)).isEqualTo(skattebeløp.setScale(0))
        assertThat(this.tilbakekrevingsbeløpEtterSkatt.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(tilbakekrevingsbeløpEtterSkatt.setScale(0))
        assertThat(this.utbetaltYtelsesbeløp.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(utbetaltYtelsesbeløp.setScale(0))
        assertThat(this.riktigYtelsesbeløp.setScale(0, RoundingMode.HALF_DOWN)).isEqualTo(riktigYtelsesbeløp.setScale(0))
    }
}
