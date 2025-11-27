package no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf

enum class Dokumentklass(
    private val beskrivelse: String,
) {
    B("Brev"),
    VB("Vedtaksbrev"),
    IB("Infobrev"),
    KA("Klage eller anke"),
}
