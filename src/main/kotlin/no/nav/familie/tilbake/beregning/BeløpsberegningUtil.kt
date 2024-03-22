package no.nav.familie.tilbake.beregning

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Periode
import java.math.BigDecimal
import java.math.RoundingMode

// TODO denne er avhengig av antall måneder?
object BeløpsberegningUtil {
    fun beregnBeløpPerMåned( // TODO PER MÅNED??
        beløp: BigDecimal,
        kravgrunnlagsperiode: Datoperiode,
    ): BigDecimal {
        return beløp.divide(BigDecimal.valueOf(kravgrunnlagsperiode.lengdeIHeleMåneder()), 2, RoundingMode.HALF_UP)
    }

    fun beregnBeløp(
        vurderingsperiode: Datoperiode,
        kravgrunnlagsperiode: Datoperiode,
        beløpPerMåned: BigDecimal,
    ): BigDecimal {
        val overlapp = kravgrunnlagsperiode.snitt(vurderingsperiode)
        if (overlapp != null) {
            return beløpPerMåned.multiply(BigDecimal.valueOf(overlapp.lengdeIHeleMåneder())) // TODO
        }
        return BigDecimal.ZERO
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
