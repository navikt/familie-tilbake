package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode

object BeløpsberegningUtil {
    fun beregnBeløpPerMåned(
        beløp: BigDecimal,
        kravgrunnlagsperiode: Datoperiode,
    ): BigDecimal = beløp.divide(BigDecimal.valueOf(kravgrunnlagsperiode.lengdeIHeleMåneder()), 2, RoundingMode.HALF_UP)

    fun beregnBeløp(
        vurderingsperiode: Datoperiode,
        kravgrunnlagsperiode: Datoperiode,
        beløpPerMåned: BigDecimal,
    ): BigDecimal {
        val overlapp = kravgrunnlagsperiode.snitt(vurderingsperiode) ?: return BigDecimal.ZERO
        return beløpPerMåned.multiply(BigDecimal.valueOf(overlapp.lengdeIHeleMåneder()))
    }

    fun beregnBeløpForPeriode(
        tilbakekrevesBeløp: BigDecimal,
        vurderingsperiode: Datoperiode,
        kravgrunnlagsperiode: Datoperiode,
    ): BigDecimal {
        val grunnlagBeløpPerMåned: BigDecimal = beregnBeløpPerMåned(tilbakekrevesBeløp, kravgrunnlagsperiode)
        val ytelseBeløp: BigDecimal = beregnBeløp(vurderingsperiode, kravgrunnlagsperiode, grunnlagBeløpPerMåned)
        return ytelseBeløp.setScale(0, RoundingMode.HALF_UP)
    }
}
