package no.nav.tilbakekreving.beregning

import java.math.BigDecimal
import java.math.RoundingMode

sealed interface Reduksjon {
    val andel: BigDecimal? get() = null

    fun beregn(kravgrunnlagBeløp: BigDecimal): BigDecimal

    class Prosentdel(override val andel: BigDecimal) : Reduksjon {
        override fun beregn(kravgrunnlagBeløp: BigDecimal): BigDecimal {
            return kravgrunnlagBeløp
                .multiply(andel)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
        }
    }

    class ManueltBeløp(val beløp: BigDecimal) : Reduksjon {
        override fun beregn(kravgrunnlagBeløp: BigDecimal): BigDecimal = beløp
    }

    class FullstendigRefusjon : Reduksjon {
        override val andel = TilbakekrevingsberegningVilkår.HUNDRE_PROSENT

        override fun beregn(kravgrunnlagBeløp: BigDecimal): BigDecimal = kravgrunnlagBeløp
    }

    class IngenTilbakekreving : Reduksjon {
        override val andel: BigDecimal? = BigDecimal.ZERO

        override fun beregn(kravgrunnlagBeløp: BigDecimal): BigDecimal = BigDecimal.ZERO
    }
}
