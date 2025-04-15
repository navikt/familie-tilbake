package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.tilbakekreving.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

object TilbakekrevingsberegningVilkår {
    val HUNDRE_PROSENT: BigDecimal = BigDecimal.valueOf(100)
    private val RENTESATS: BigDecimal = BigDecimal.valueOf(10)
    private val RENTEFAKTOR: BigDecimal = RENTESATS.divide(HUNDRE_PROSENT, 2, RoundingMode.UNNECESSARY)

    // TODO: Flytt test og gjør internal
    fun beregn(
        vilkårVurdering: VilkårsvurdertPeriodeAdapter,
        delresultat: FordeltKravgrunnlagsbeløp,
        perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
        beregnRenter: Boolean,
    ): Beregningsresultatsperiode {
        val periode = vilkårVurdering.periode()
        val vurdering = vilkårVurdering.vurdering()
        val renter = beregnRenter && vilkårVurdering.renter()
        val reduksjon = vilkårVurdering.reduksjon()
        val beløpUtenRenter =
            if (vilkårVurdering.ignoreresPgaLavtBeløp()) {
                BigDecimal.ZERO
            } else {
                reduksjon.beregn(delresultat.feilutbetaltBeløp)
            }
        val rentebeløp = beregnRentebeløp(beløpUtenRenter, renter)
        val tilbakekrevingBeløp = beløpUtenRenter.add(rentebeløp)
        val skattBeløp = beregnSkattBeløp(periode, beløpUtenRenter, perioderMedSkatteprosent)
        val nettoBeløp = tilbakekrevingBeløp.subtract(skattBeløp)
        return Beregningsresultatsperiode(
            periode = periode,
            vurdering = vurdering,
            renteprosent = if (renter) RENTESATS else null,
            feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
            riktigYtelsesbeløp = delresultat.riktigYtelsesbeløp,
            utbetaltYtelsesbeløp = delresultat.utbetaltYtelsesbeløp,
            andelAvBeløp = reduksjon.andel,
            manueltSattTilbakekrevingsbeløp = (reduksjon as? Reduksjon.ManueltBeløp)?.beløp,
            tilbakekrevingsbeløpUtenRenter = beløpUtenRenter,
            rentebeløp = rentebeløp,
            tilbakekrevingsbeløpEtterSkatt = nettoBeløp,
            skattebeløp = skattBeløp,
            tilbakekrevingsbeløp = tilbakekrevingBeløp,
        )
    }

    private fun beregnRentebeløp(
        beløp: BigDecimal,
        renter: Boolean,
    ): BigDecimal = if (renter) beløp.multiply(RENTEFAKTOR).setScale(0, RoundingMode.DOWN) else BigDecimal.ZERO

    private fun beregnSkattBeløp(
        periode: Datoperiode,
        bruttoTilbakekrevesBeløp: BigDecimal,
        perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
    ): BigDecimal {
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
    }
}
