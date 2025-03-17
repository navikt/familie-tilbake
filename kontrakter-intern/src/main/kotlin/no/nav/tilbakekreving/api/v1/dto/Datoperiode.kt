package no.nav.tilbakekreving.api.v1.dto

import java.math.BigDecimal
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class BeregnetPeriodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
)

data class BeregnetPerioderDto(
    val beregnetPerioder: List<BeregnetPeriodeDto>,
)
