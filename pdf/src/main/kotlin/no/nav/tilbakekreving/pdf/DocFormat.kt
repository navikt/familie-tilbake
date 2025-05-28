package no.nav.tilbakekreving.pdf

import java.util.Locale

enum class DocFormat {
    PDF,
    HTML,
    EMAIL,
    ;

    override fun toString(): String = name.lowercase(Locale.getDefault())
}
