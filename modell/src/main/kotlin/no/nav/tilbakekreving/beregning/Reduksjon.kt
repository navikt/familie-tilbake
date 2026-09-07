package no.nav.tilbakekreving.beregning

import java.math.BigDecimal

sealed interface Reduksjon {
    val andelTilbakekreves: BigDecimal? get() = null

    fun beregn(
        kravgrunnlagBeløp: BigDecimal,
        andelAvBeløp: BigDecimal,
    ): BigDecimal

    class Prosentdel(override val andelTilbakekreves: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal {
            return kravgrunnlagBeløp
                .multiply(andelTilbakekreves)
                .divide(HUNDRE_PROSENT)
        }
    }

    class ProsentdelAvBeløpIBehold(override val andelTilbakekreves: BigDecimal, val beløp: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal {
            return beløp.multiply(andelAvBeløp)
                .multiply(andelTilbakekreves)
                .divide(HUNDRE_PROSENT)
        }
    }

    class ManueltBeløp(val beløp: BigDecimal) : Reduksjon {
        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal = beløp.multiply(andelAvBeløp)
    }

    class FullstendigTilbakekreving : Reduksjon {
        override val andelTilbakekreves = HUNDRE_PROSENT

        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal = kravgrunnlagBeløp
    }

    class IngenTilbakekreving : Reduksjon {
        override val andelTilbakekreves: BigDecimal = BigDecimal.ZERO

        override fun beregn(
            kravgrunnlagBeløp: BigDecimal,
            andelAvBeløp: BigDecimal,
        ): BigDecimal = BigDecimal.ZERO
    }
}
