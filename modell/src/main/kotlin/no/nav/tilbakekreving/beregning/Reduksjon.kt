package no.nav.tilbakekreving.beregning

import java.math.BigDecimal

sealed interface Reduksjon {
    val andelTilbakekreves: BigDecimal? get() = null

    fun beregn(totaltFeilutbetaltBeløp: BigDecimal): BigDecimal

    class Prosentdel(override val andelTilbakekreves: BigDecimal) : Reduksjon {
        override fun beregn(totaltFeilutbetaltBeløp: BigDecimal): BigDecimal {
            return totaltFeilutbetaltBeløp
                .multiply(andelTilbakekreves)
                .divide(HUNDRE_PROSENT)
        }
    }

    class ProsentdelAvBeløpIBehold(override val andelTilbakekreves: BigDecimal, val beløp: BigDecimal) : Reduksjon {
        override fun beregn(totaltFeilutbetaltBeløp: BigDecimal): BigDecimal {
            return beløp
                .multiply(andelTilbakekreves)
                .divide(HUNDRE_PROSENT)
        }
    }

    class ManueltBeløp(val beløp: BigDecimal) : Reduksjon {
        override fun beregn(totaltFeilutbetaltBeløp: BigDecimal): BigDecimal = beløp
    }

    class FullstendigTilbakekreving : Reduksjon {
        override val andelTilbakekreves = HUNDRE_PROSENT

        override fun beregn(totaltFeilutbetaltBeløp: BigDecimal): BigDecimal = totaltFeilutbetaltBeløp
    }

    class IngenTilbakekreving : Reduksjon {
        override val andelTilbakekreves: BigDecimal = BigDecimal.ZERO

        override fun beregn(totaltFeilutbetaltBeløp: BigDecimal): BigDecimal = BigDecimal.ZERO
    }
}
