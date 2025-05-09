package no.nav.tilbakekreving.beregning.delperiode

import java.math.BigDecimal
import java.math.RoundingMode
import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering

class Foreldet(
    private val periode: Datoperiode,
    private val andel: Andel,
) : Delperiode {
    override fun feilutbetalt(): BigDecimal {
        return andel.feilutbetaltBeløp()
    }

    override fun tilbakekreves(): BigDecimal {
        return BigDecimal.ZERO
    }

    override fun beregningsresultat(): Beregningsresultatsperiode {
        return Beregningsresultatsperiode(
            periode = periode,
            feilutbetaltBeløp = andel.feilutbetaltBeløp().setScale(0, RoundingMode.HALF_UP),
            riktigYtelsesbeløp = andel.riktigYtelsesbeløp().setScale(0, RoundingMode.HALF_UP),
            utbetaltYtelsesbeløp = andel.utbetaltYtelsesbeløp().setScale(0, RoundingMode.HALF_UP),
            tilbakekrevingsbeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
            rentebeløp = BigDecimal.ZERO,
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }

    companion object {
        fun opprett(
            periode: Datoperiode,
            kravgrunnlagPeriode: KravgrunnlagPeriodeAdapter,
        ): Foreldet {
            val delperiode = kravgrunnlagPeriode.periode().snitt(periode)!!
            return Foreldet(delperiode, Andel(kravgrunnlagPeriode, delperiode))
        }
    }
}
