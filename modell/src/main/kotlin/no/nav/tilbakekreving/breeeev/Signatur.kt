package no.nav.tilbakekreving.breeeev

data class Signatur(
    val ansvarligSaksbehandlerIdent: String,
    val ansvarligBeslutterIdent: String?,
    val ansvarligEnhet: String,
)
