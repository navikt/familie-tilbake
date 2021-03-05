package no.nav.familie.tilbake.service.pdfgen

enum class DocFormat {
    PDF,
    HTML,
    EMAIL;

    override fun toString(): String {
        return name.toLowerCase()
    }
}
