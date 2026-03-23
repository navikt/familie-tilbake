package no.nav.tilbakekreving.tekst

fun Collection<String>.slåSammen(): String {
    if (size == 1) return last()
    return take(size - 1).joinToString(", ", postfix = " og ") + last()
}
