package no.nav.tilbakekreving.beregning.delperiode

import no.nav.tilbakekreving.beregning.HUNDRE_PROSENT
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Delperiode.Beløp.Companion.forKlassekode
import no.nav.tilbakekreving.beregning.delperiode.JusterbartBeløp.Companion.fordelSkattebeløp
import no.nav.tilbakekreving.beregning.delperiode.JusterbartBeløp.Companion.fordelTilbakekrevingsbeløp
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

class Vilkårsvurdert(
    private val vurdering: VilkårsvurdertPeriodeAdapter,
    private val beregnRenter: Boolean,
    kravgrunnlagPerioder: List<KravgrunnlagPeriodeAdapter>,
) : Vurderingsperiode<Vilkårsvurdert.Utbetalingsperiode> {
    override val periode get() = vurdering.periode()
    override val delperioder: List<Utbetalingsperiode> = kravgrunnlagPerioder.map { kravgrunnlagPeriode ->
        val delperiode = requireNotNull(kravgrunnlagPeriode.periode().snitt(vurdering.periode())) {
            "Finner ingen kravgrunnlagsperiode som er dekket av vilkårsvurderingsperioden ${vurdering.periode()}, kravgrunnlagsperiode=${kravgrunnlagPeriode.periode()}"
        }
        val totaltFeilutbetaltBeløp = kravgrunnlagPerioder.sumOf { it.beløpTilbakekreves().sumOf { beløp -> beløp.tilbakekrevesBeløp() } }
        Utbetalingsperiode(
            periode = delperiode,
            beløp = kravgrunnlagPeriode.beløpTilbakekreves().map { beløp ->
                JusterbartBeløp(
                    klassekode = beløp.klassekode(),
                    periode = delperiode,
                    beløpTilbakekreves = beløp,
                    reduksjon = vurdering.reduksjon(),
                    andelAvBeløp = beløp.tilbakekrevesBeløp().divide(totaltFeilutbetaltBeløp, 6, RoundingMode.HALF_DOWN),
                )
            },
            kravgrunnlagPeriode = kravgrunnlagPeriode,
            beregnRenter = beregnRenter,
            vurdering = vurdering,
        )
    }

    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = vurdering.periode(),
            vurdering = vurdering.vurdering(),
            renteprosent = if (beregnRenter && vurdering.renter()) RENTESATS else null,
            feilutbetaltBeløp = delperioder.sumOf { it.feilutbetaltBeløp() },
            riktigYtelsesbeløp = delperioder.sumOf { it.summer(JusterbartBeløp::riktigYtelsesbeløp) },
            utbetaltYtelsesbeløp = delperioder.sumOf { it.summer(JusterbartBeløp::utbetaltYtelsesbeløp) },
            andelAvBeløp = vurdering.reduksjon().andel,
            manueltSattTilbakekrevingsbeløp = (vurdering.reduksjon() as? Reduksjon.ManueltBeløp)?.beløp,
            tilbakekrevingsbeløpUtenRenter = delperioder.sumOf { it.summer(JusterbartBeløp::tilbakekrevesBrutto) },
            rentebeløp = delperioder.sumOf { it.renter() },
            tilbakekrevingsbeløpEtterSkatt = delperioder.sumOf { it.tilbakekrevesNetto() },
            skattebeløp = delperioder.sumOf { it.summer(JusterbartBeløp::skatt) },
            tilbakekrevingsbeløp = delperioder.sumOf { it.tilbakekrevesBruttoMedRenter() },
        )
    }

    class Utbetalingsperiode(
        override val periode: Datoperiode,
        private val beløp: List<JusterbartBeløp>,
        private val kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
        private val beregnRenter: Boolean,
        private val vurdering: VilkårsvurdertPeriodeAdapter,
    ) : Delperiode {
        private var rentebeløpAvrunding = BigDecimal.ZERO
        private val rentebeløp = beregnRentebeløp(beløp.sumOf { it.tilbakekrevingsbeløp })

        fun summer(hentBeløp: JusterbartBeløp.() -> BigDecimal) = beløp.sumOf { it.hentBeløp() }

        override fun beløpForKlassekode(klassekode: String): Delperiode.Beløp = beløp.forKlassekode(klassekode)

        override fun tilbakekrevesBruttoMedRenter(): BigDecimal = beløp().sumOf { it.tilbakekrevesBrutto() } + renter()

        fun tilbakekrevesNetto(): BigDecimal = tilbakekrevesBruttoMedRenter() - beløp().sumOf { it.skatt() }

        override fun renter(): BigDecimal = rentebeløp.setScale(0, RoundingMode.DOWN) + rentebeløpAvrunding

        override fun beløp(): List<Delperiode.Beløp> = beløp

        private fun beregnRentebeløp(
            beløp: BigDecimal,
        ): BigDecimal = if (beregnRenter && vurdering.renter()) {
            beløp.multiply(RENTEFAKTOR)
        } else {
            BigDecimal.ZERO
        }

        override fun feilutbetaltBeløp(): BigDecimal = kravgrunnlagPeriode.feilutbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP)

        companion object {
            fun <T : Iterable<Utbetalingsperiode>> T.fordelTilbakekrevingsbeløp(): T = apply {
                filterIsInstance<Utbetalingsperiode>().flatMap { it.beløp }.fordelTilbakekrevingsbeløp()
            }

            fun <T : Iterable<Utbetalingsperiode>> T.fordelRentebeløp(): T = apply {
                fordel(Utbetalingsperiode::rentebeløp, Utbetalingsperiode::periode, RoundingMode.DOWN) {
                    rentebeløpAvrunding = BigDecimal.ONE
                }
            }

            fun <T : Iterable<Utbetalingsperiode>> T.fordelSkattebeløp(): T = apply {
                filterIsInstance<Utbetalingsperiode>().flatMap { it.beløp }.fordelSkattebeløp()
            }
        }
    }

    companion object {
        private val RENTESATS = BigDecimal.valueOf(10)
        private val RENTEFAKTOR = RENTESATS.divide(HUNDRE_PROSENT, 4, RoundingMode.HALF_DOWN)

        fun opprett(
            vurdering: VilkårsvurdertPeriodeAdapter,
            kravgrunnlagPerioder: List<KravgrunnlagPeriodeAdapter>,
            beregnRenter: Boolean,
        ): Vilkårsvurdert {
            return Vilkårsvurdert(
                vurdering = vurdering,
                beregnRenter = beregnRenter,
                kravgrunnlagPerioder = kravgrunnlagPerioder,
            )
        }
    }
}
