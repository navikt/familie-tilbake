package no.nav.tilbakekreving.beregning.delperiode

import java.math.BigDecimal
import java.math.RoundingMode
import no.nav.tilbakekreving.beregning.HUNDRE_PROSENT
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

class Vilkårsvurdert(
    private val periode: Datoperiode,
    private val andel: Andel,
    private val vurdering: VilkårsvurdertPeriodeAdapter,
    private val beregnRenter: Boolean,
    private val antallKravgrunnlagGjelder: Int,
) : Delperiode {
    private val RENTESATS = BigDecimal.valueOf(10)
    private val RENTEFAKTOR = RENTESATS.divide(HUNDRE_PROSENT, 4, RoundingMode.HALF_DOWN)
    override fun feilutbetalt(): BigDecimal {
        return andel.feilutbetaltBeløp()
    }

    override fun tilbakekreves(): BigDecimal {
        return if (vurdering.ignoreresPgaLavtBeløp()) {
            BigDecimal.ZERO
        } else {
            vurdering.reduksjon().beregn(andel.feilutbetaltBeløp(), antallKravgrunnlagGjelder)
        }
    }

    override fun beregningsresultat(): Beregningsresultatsperiode {
        val reduksjon = vurdering.reduksjon()
        val bruttoBeløp = tilbakekreves()
        val rentebeløp = beregnRentebeløp(bruttoBeløp)
        val tilbakekrevingBeløp = bruttoBeløp.add(rentebeløp)
        val skattBeløp = beregnSkattebeløp(bruttoBeløp)
        val nettoBeløp = tilbakekrevingBeløp.subtract(skattBeløp.setScale(0, RoundingMode.DOWN))

        return Beregningsresultatsperiode(
            periode = periode,
            vurdering = vurdering.vurdering(),
            renteprosent = if (beregnRenter && vurdering.renter()) RENTESATS else null,
            feilutbetaltBeløp = andel.feilutbetaltBeløp(),
            riktigYtelsesbeløp = andel.riktigYtelsesbeløp(),
            utbetaltYtelsesbeløp = andel.utbetaltYtelsesbeløp(),
            andelAvBeløp = reduksjon.andel,
            manueltSattTilbakekrevingsbeløp = tilbakekrevingBeløp.takeIf { vurdering.reduksjon() is Reduksjon.ManueltBeløp },
            tilbakekrevingsbeløpUtenRenter = bruttoBeløp,
            rentebeløp = rentebeløp,
            tilbakekrevingsbeløpEtterSkatt = nettoBeløp,
            skattebeløp = skattBeløp,
            tilbakekrevingsbeløp = tilbakekrevingBeløp,
        )
    }

    private fun beregnRentebeløp(
        beløp: BigDecimal,
    ): BigDecimal = if (beregnRenter && vurdering.renter()) {
        beløp.multiply(RENTEFAKTOR)
    } else {
        BigDecimal.ZERO
    }

    private fun beregnSkattebeløp(
        tilbakekrevesBeløp: BigDecimal,
    ): BigDecimal {
        if (tilbakekrevesBeløp.isZero()) return BigDecimal.ZERO
        val forskjellMedRenter = tilbakekrevesBeløp.divide(andel.feilutbetaltBeløp(), 4, RoundingMode.HALF_UP)
        return andel.skatt() * forskjellMedRenter
    }

    /*
            val totalKgTilbakekrevesBeløp = perioderMedSkatteprosent.sumOf { it.tilbakekrevingsbeløp }
        if (totalKgTilbakekrevesBeløp.isZero()) return BigDecimal.ZERO
        val andel = bruttoTilbakekrevesBeløp.divide(totalKgTilbakekrevesBeløp, 4, RoundingMode.HALF_UP)

        return perioderMedSkatteprosent
            .filter { periode.overlapper(it.periode) }
            .sumOf { grunnlagPeriode ->
                grunnlagPeriode.tilbakekrevingsbeløp
                    .multiply(andel)
                    .multiply(grunnlagPeriode.skatteprosent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.DOWN)
            }
     */

    companion object {
        fun opprett(
            vurdering: VilkårsvurdertPeriodeAdapter,
            kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
            beregnRenter: Boolean,
            antallKravgrunnlagGjelder: Int,
        ): Vilkårsvurdert {
            val delperiode = kravgrunnlagPeriode.periode().snitt(vurdering.periode())!!
            return Vilkårsvurdert(
                periode = delperiode,
                andel = Andel(
                    kravgrunnlagPeriode = kravgrunnlagPeriode,
                    delperiode = delperiode,
                ),
                vurdering = vurdering,
                beregnRenter = beregnRenter,
                antallKravgrunnlagGjelder = antallKravgrunnlagGjelder,
            )
        }
    }
}
