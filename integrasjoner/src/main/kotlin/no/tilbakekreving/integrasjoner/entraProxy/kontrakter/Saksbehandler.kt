package no.tilbakekreving.integrasjoner.entraProxy.kontrakter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Saksbehandler(
    val navIdent: String,
    val visningNavn: String,
    val fornavn: String,
    val etternavn: String,
    val epost: String,
    val enhet: Enhet,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Enhet(
    val enhetnummer: String,
)
