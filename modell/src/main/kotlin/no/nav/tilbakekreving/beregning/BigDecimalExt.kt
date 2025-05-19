package no.nav.tilbakekreving.beregning

import java.math.BigDecimal

fun BigDecimal.isZero() = this.signum() == 0

fun BigDecimal.fraksjon(): BigDecimal = remainder(BigDecimal.ONE)

val HUNDRE_PROSENT = BigDecimal.valueOf(100)
