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
    override val periode: Datoperiode,
    override val vurdertPeriode: Datoperiode,
    private val vurdering: VilkårsvurdertPeriodeAdapter,
    private val beregnRenter: Boolean,
    private val kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
    private val beløp: List<JusterbartBeløp>,
) : Delperiode {
    private var rentebeløpAvrunding = BigDecimal.ZERO
    private val rentebeløp = beregnRentebeløp(beløp.sumOf { it.tilbakekrevingsbeløp })

    override fun beløpForKlassekode(klassekode: String): Delperiode.Beløp = beløp.forKlassekode(klassekode)

    override fun tilbakekrevesBruttoMedRenter(): BigDecimal = beløp().sumOf { it.tilbakekrevesBrutto() } + renter()

    private fun tilbakekrevesNetto(): BigDecimal = tilbakekrevesBruttoMedRenter() - beløp().sumOf { it.skatt() }

    override fun renter(): BigDecimal = rentebeløp.setScale(0, RoundingMode.DOWN) + rentebeløpAvrunding

    override fun beløp(): List<Delperiode.Beløp> = beløp

    override fun feilutbetaltBeløp(): BigDecimal = kravgrunnlagPeriode.feilutbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP)

    fun validerIkkeManueltBeløpOgUlikeKlassekoder() {
        if (vurdering.reduksjon() is Reduksjon.ManueltBeløp && beløp.size > 1) {
            throw RuntimeException("Fant periode med manuelt satt tilbakekrevingsbeløp og flere beløp i perioden. Denne koden har nå en bug som må fikses.")
        }
    }

    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            vurdering = vurdering.vurdering(),
            renteprosent = if (beregnRenter && vurdering.renter()) RENTESATS else null,
            feilutbetaltBeløp = feilutbetaltBeløp(),
            riktigYtelsesbeløp = beløp.sumOf { it.riktigYtelsesbeløp() },
            utbetaltYtelsesbeløp = beløp.sumOf { it.utbetaltYtelsesbeløp() },
            andelAvBeløp = vurdering.reduksjon().andel,
            manueltSattTilbakekrevingsbeløp = (vurdering.reduksjon() as? Reduksjon.ManueltBeløp)?.beløp,
            tilbakekrevingsbeløpUtenRenter = beløp().sumOf { it.tilbakekrevesBrutto() },
            rentebeløp = renter(),
            tilbakekrevingsbeløpEtterSkatt = tilbakekrevesNetto(),
            skattebeløp = beløp().sumOf { it.skatt() },
            tilbakekrevingsbeløp = tilbakekrevesBruttoMedRenter(),
        )
    }

    private fun beregnRentebeløp(
        beløp: BigDecimal,
    ): BigDecimal = if (beregnRenter && vurdering.renter()) {
        beløp.multiply(RENTEFAKTOR)
    } else {
        BigDecimal.ZERO
    }

    companion object {
        private val RENTESATS = BigDecimal.valueOf(10)
        private val RENTEFAKTOR = RENTESATS.divide(HUNDRE_PROSENT, 4, RoundingMode.HALF_DOWN)

        fun opprett(
            vurdering: VilkårsvurdertPeriodeAdapter,
            kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
            beregnRenter: Boolean,
            antallKravgrunnlagGjelder: Int,
        ): Vilkårsvurdert {
            val delperiode = requireNotNull(kravgrunnlagPeriode.periode().snitt(vurdering.periode())) {
                "Finner ingen kravgrunnlagsperiode som er dekket av vilkårsvurderingsperioden ${vurdering.periode()}, kravgrunnlagsperiode=${kravgrunnlagPeriode.periode()}"
            }
            return Vilkårsvurdert(
                periode = delperiode,
                vurdertPeriode = vurdering.periode(),
                vurdering = vurdering,
                beregnRenter = beregnRenter,
                beløp = kravgrunnlagPeriode.beløpTilbakekreves().map {
                    JusterbartBeløp(it.klassekode(), delperiode, it, vurdering, antallKravgrunnlagGjelder)
                },
                kravgrunnlagPeriode = kravgrunnlagPeriode,
            )
        }

        fun <T : Iterable<Delperiode>> T.fordelRentebeløp(): T = apply {
            fordel(Vilkårsvurdert::rentebeløp, Vilkårsvurdert::periode, RoundingMode.DOWN) {
                rentebeløpAvrunding = BigDecimal.ONE
            }
        }

        fun <T : Iterable<Delperiode>> T.fordelTilbakekrevingsbeløp(): T = apply {
            filterIsInstance<Vilkårsvurdert>().flatMap { it.beløp }.fordelTilbakekrevingsbeløp()
        }

        fun <T : Iterable<Delperiode>> T.fordelSkattebeløp(): T = apply {
            filterIsInstance<Vilkårsvurdert>().flatMap { it.beløp }.fordelSkattebeløp()
        }
    }
}
