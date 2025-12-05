package no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf

enum class DokumentKlasse(
    private val beskrivelse: String,
) {
    B("Brev"),
    VB("Vedtaksbrev"),
    IB("Infobrev"),
    KA("Klage eller anke"),
}
