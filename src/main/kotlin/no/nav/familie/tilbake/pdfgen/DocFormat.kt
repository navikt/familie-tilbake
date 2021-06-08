package no.nav.familie.tilbake.pdfgen

enum class DocFormat {
    PDF,
    HTML,
    EMAIL;

    override fun toString(): String {
        return name.toLowerCase()
    }
}
