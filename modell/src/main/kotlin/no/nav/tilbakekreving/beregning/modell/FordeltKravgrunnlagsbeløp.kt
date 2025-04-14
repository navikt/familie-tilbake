package no.nav.tilbakekreving.beregning.modell

import java.math.BigDecimal

class FordeltKravgrunnlagsbeløp(
    val feilutbetaltBeløp: BigDecimal,
    val utbetaltYtelsesbeløp: BigDecimal,
    val riktigYtelsesbeløp: BigDecimal,
)
