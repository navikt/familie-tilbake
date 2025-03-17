package no.nav.tilbakekreving.kontrakter

enum class Behandlingstype(
    val visningsnavn: String,
) {
    TILBAKEKREVING("Tilbakekreving"),
    REVURDERING_TILBAKEKREVING("Tilbakekreving revurdering"),
}
