package no.nav.tilbakekreving.entities

import java.math.BigDecimal

data class PeriodeEntity(
    val periode: DatoperiodeEntity,
    val månedligSkattebeløp: BigDecimal,
    val ytelsesbeløp: List<BeløpEntity>,
    val feilutbetaltBeløp: List<BeløpEntity>,
)
