package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

data class BeregnetPeriodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
)

data class BeregnetPerioderDto(
    val beregnetPerioder: List<BeregnetPeriodeDto>,
)
