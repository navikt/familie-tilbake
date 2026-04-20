package no.nav.tilbakekreving.tekst

fun Collection<String>.slåSammen(
    separator: String = " og ",
): String {
    if (size == 1) return last()
    return take(size - 1).joinToString(", ", postfix = separator) + last()
}
