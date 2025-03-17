package no.nav.tilbakekreving.kontrakter

import jakarta.validation.constraints.Pattern

data class Institusjon(
    @field:Pattern(regexp = "(^$|.{9})", message = "Organisasjonsnummer for institusjon er ikke riktig")
    val organisasjonsnummer: String,
)
