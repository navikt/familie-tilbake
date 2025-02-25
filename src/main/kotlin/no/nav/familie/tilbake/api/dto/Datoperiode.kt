package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.kontrakter.Datoperiode
import java.math.BigDecimal

data class BeregnetPeriodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
)

data class BeregnetPerioderDto(
    val beregnetPerioder: List<BeregnetPeriodeDto>,
)
