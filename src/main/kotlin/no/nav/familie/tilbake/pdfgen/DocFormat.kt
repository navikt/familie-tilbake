package no.nav.familie.tilbake.pdfgen

import java.util.Locale

enum class DocFormat {
    PDF,
    HTML,
    EMAIL,
    ;

    override fun toString(): String = name.lowercase(Locale.getDefault())
}
