package no.nav.tilbakekreving.kontrakter.verge

import jakarta.validation.constraints.Pattern

data class Verge(
    val vergetype: Vergetype,
    val navn: String,
    @field:Pattern(regexp = "(^$|.{9})", message = "Organisasjonsnummer er ikke riktig")
    val organisasjonsnummer: String? = null,
    @field:Pattern(regexp = "(^$|.{11})", message = "PersonIdent er ikke riktig")
    val personIdent: String? = null,
)
