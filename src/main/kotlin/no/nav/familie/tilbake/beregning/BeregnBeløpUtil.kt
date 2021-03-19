package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal
import java.math.RoundingMode

object BeregnBeløpUtil {

    fun beregnBeløpPerMåned(beløp: BigDecimal, kravgrunnlagsperiode: Periode): BigDecimal {
        return beløp.divide(BigDecimal.valueOf(kravgrunnlagsperiode.lengdeIMåneder()), 2, RoundingMode.HALF_UP)
    }

    fun beregnBeløp(vurderingsperiode: Periode, kravgrunnlagsperiode: Periode, beløpPerMåned: BigDecimal): BigDecimal {
        val overlapp = kravgrunnlagsperiode.snitt(vurderingsperiode)
        if (overlapp != null) {
            return beløpPerMåned.multiply(BigDecimal.valueOf(overlapp.lengdeIMåneder()))
        }
        return BigDecimal.ZERO
    }

    fun beregnBeløpForPeriode(tilbakekrevesBeløp: BigDecimal,
                              vurderingsperiode: Periode,
                              kravgrunnlagsperiode: Periode): BigDecimal {
        val grunnlagBeløpPerMåned: BigDecimal = beregnBeløpPerMåned(tilbakekrevesBeløp, kravgrunnlagsperiode)
        val ytelseBeløp: BigDecimal = beregnBeløp(vurderingsperiode, kravgrunnlagsperiode, grunnlagBeløpPerMåned)
        return ytelseBeløp.setScale(0, RoundingMode.HALF_UP)
    }

}