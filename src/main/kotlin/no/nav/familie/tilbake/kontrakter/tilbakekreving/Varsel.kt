package no.nav.familie.tilbake.kontrakter.tilbakekreving

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class Varsel(
    @field:NotBlank(message = "Mangler varseltekst")
    val varseltekst: String,
    @field:Min(value = 1, message = "sumFeilutbetaling må være større enn null")
    val sumFeilutbetaling: BigDecimal,
    @field:Size(min = 1, message = "Periode kan ikke være tom")
    val perioder: List<Periode>,
)
