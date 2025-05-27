package no.nav.tilbakekreving.entities

import java.math.BigDecimal

data class BeløpEntity(
    val klassekode: String,
    val klassetype: String,
    val opprinneligUtbetalingsbeløp: BigDecimal,
    val nyttBeløp: BigDecimal,
    val tilbakekrevesBeløp: BigDecimal,
    val skatteprosent: BigDecimal,
)
