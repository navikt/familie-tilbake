package no.nav.tilbakekreving.beregning

import java.math.BigDecimal

sealed interface Reduksjon {
    val andel: BigDecimal? get() = null

    fun beregn(
        kravgrunnlagBeløp: BigDecimal,
        andelAvBeløp: BigDecimal,
    ): BigDecimal

    class Prosentdel(override val andel: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal {
            return kravgrunnlagBeløp
                .multiply(andel)
                .divide(HUNDRE_PROSENT)
        }
    }

    class ManueltBeløp(val beløp: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal = beløp.multiply(andelAvBeløp)
    }

    class FullstendigRefusjon : Reduksjon {
        override val andel = HUNDRE_PROSENT

        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal = kravgrunnlagBeløp
    }

    class IngenTilbakekreving : Reduksjon {
        override val andel: BigDecimal = BigDecimal.ZERO

        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal = BigDecimal.ZERO
    }
}
