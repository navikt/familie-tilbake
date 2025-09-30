package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

enum class Dokumentkategori(
    private val beskrivelse: String,
) {
    B("Brev"),
    VB("Vedtaksbrev"),
    IB("Infobrev"),
    KA("Klage eller anke"),
}

enum class Behandlingstema(
    val value: String,
) {
    Tilbakebetaling("ab0007"),
}
