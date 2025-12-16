package no.tilbakekreving.integrasjoner.entraProxy.kontrakter

data class Saksbehandler(
    val navIdent: String,
    val visningNavn: String,
    val fornavn: String,
    val etternavn: String,
    val epost: String,
    val enhet: Enhet,
    val tIdent: String,
)

data class Enhet(
    val enhetnummer: String,
    val navn: String,
)
