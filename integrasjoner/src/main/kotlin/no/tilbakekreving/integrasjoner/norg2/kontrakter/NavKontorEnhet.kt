package no.tilbakekreving.integrasjoner.norg2.kontrakter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NavKontorEnhet(
    val enhetId: Int,
    val navn: String,
    val enhetNr: String,
    val status: String,
)
