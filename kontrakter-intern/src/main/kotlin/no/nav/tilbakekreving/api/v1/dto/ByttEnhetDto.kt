package no.nav.tilbakekreving.api.v1.dto

import jakarta.validation.constraints.Size

data class ByttEnhetDto(
    val enhet: String,
    @Size(max = 400, message = "Begrunnelse er for lang")
    val begrunnelse: String,
)
