package no.tilbakekreving.integrasjoner.tilgangsmaskinen.kontrakter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonDetailResponseDTO(
    val title: AvvisningskodeDTO,
    val begrunnelse: String,
    val traceId: String,
    val brukerIdent: String,
    val navIdent: String,
    val kanOverstyres: Boolean,
)
