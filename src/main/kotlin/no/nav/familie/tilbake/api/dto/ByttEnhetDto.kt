package no.nav.familie.tilbake.api.dto

import javax.validation.constraints.Size

data class ByttEnhetDto(
    val enhet: String,
    @Size(max = 400, message = "Begrunnelse er for lang")
    val begrunnelse: String,
)
