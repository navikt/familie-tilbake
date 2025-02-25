package no.nav.familie.tilbake.kontrakter.tilbakekreving

import jakarta.validation.constraints.Pattern

data class Verge(
    val vergetype: Vergetype,
    val navn: String,
    @field:Pattern(regexp = "(^$|.{9})", message = "Organisasjonsnummer er ikke riktig")
    val organisasjonsnummer: String? = null,
    @field:Pattern(regexp = "(^$|.{11})", message = "PersonIdent er ikke riktig")
    val personIdent: String? = null,
)

enum class Vergetype(
    val navn: String,
) {
    VERGE_FOR_BARN("Verge for barn under 18 år"),
    ADVOKAT("Advokat/advokatfullmektig"),
}
