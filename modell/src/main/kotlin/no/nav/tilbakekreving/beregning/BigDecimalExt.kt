package no.nav.tilbakekreving.beregning

import java.math.BigDecimal

fun BigDecimal.isNotZero() = this.signum() != 0

fun BigDecimal.isZero() = this.signum() == 0

fun BigDecimal.isGreaterThanZero() = this.signum() > 0

fun BigDecimal.isLessThanZero() = this.signum() < 0
