package no.nav.familie.tilbake.brev.pdfgen

enum class DocFormat {
    PDF,
    HTML,
    EMAIL;

    override fun toString(): String {
        return name.toLowerCase()
    }
}
