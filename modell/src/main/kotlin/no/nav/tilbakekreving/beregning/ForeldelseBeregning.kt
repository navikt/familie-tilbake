package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import java.math.BigDecimal

class ForeldelseBeregning(
    private val kravbeløpPerPeriode: Map<Datoperiode, FordeltKravgrunnlagsbeløp>,
    private val foreldetPerioder: List<Datoperiode>,
) {
    internal fun beregn(): List<Beregningsresultatsperiode> = foreldetPerioder.map(::beregnForeldetPeriode)

    private fun beregnForeldetPeriode(periode: Datoperiode): Beregningsresultatsperiode {
        val delresultat = kravbeløpPerPeriode[periode] ?: error("Periode i finnes ikke i map beløpPerPeriode")

        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
            riktigYtelsesbeløp = delresultat.riktigYtelsesbeløp,
            utbetaltYtelsesbeløp = delresultat.utbetaltYtelsesbeløp,
            tilbakekrevingsbeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
            rentebeløp = BigDecimal.ZERO,
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }
}
