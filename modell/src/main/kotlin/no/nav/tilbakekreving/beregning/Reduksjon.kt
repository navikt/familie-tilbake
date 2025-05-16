package no.nav.tilbakekreving.beregning

import java.math.BigDecimal
import java.math.RoundingMode

sealed interface Reduksjon {
    val andel: BigDecimal? get() = null

    fun beregn(
        kravgrunnlagBeløp: BigDecimal,
        antallPerioderForVilkårsvurdering: Int,
    ): BigDecimal

    class Prosentdel(override val andel: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            antallPerioderForVilkårsvurdering: Int,
        ): BigDecimal {
            return kravgrunnlagBeløp
                .multiply(andel)
                .divide(HUNDRE_PROSENT)
        }
    }

    class ManueltBeløp(val beløp: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            antallPerioderForVilkårsvurdering: Int,
        ): BigDecimal = beløp.divide(antallPerioderForVilkårsvurdering.toBigDecimal(), 6, RoundingMode.HALF_DOWN)
    }

    class FullstendigRefusjon : Reduksjon {
        override val andel = HUNDRE_PROSENT

        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            antallPerioderForVilkårsvurdering: Int,
        ): BigDecimal = kravgrunnlagBeløp
    }

    class IngenTilbakekreving : Reduksjon {
        override val andel: BigDecimal = BigDecimal.ZERO

        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            antallPerioderForVilkårsvurdering: Int,
        ): BigDecimal = BigDecimal.ZERO
    }
}
