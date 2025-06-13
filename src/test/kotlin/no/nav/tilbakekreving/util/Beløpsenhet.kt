package no.nav.tilbakekreving.util

import java.math.BigDecimal

val Int.kroner get() = BigDecimal(this)
val Double.kroner get() = BigDecimal(this).setScale(2)
val Int.prosent get() = BigDecimal(this)
