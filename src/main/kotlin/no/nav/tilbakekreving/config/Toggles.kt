package no.nav.tilbakekreving.config

data class Toggles(
    val nyModellEnabled: Boolean,
    val tilgangsmaskinenEnabled: Boolean = false,
)
