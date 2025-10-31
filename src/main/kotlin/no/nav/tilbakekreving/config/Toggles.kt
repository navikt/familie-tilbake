package no.nav.tilbakekreving.config

data class Toggles(
    val nyModellEnabled: Boolean,
    val tilgangsmaskinenEnabled: Boolean = false,
    val revurdering: Boolean = false,
    val manuellOpprettelse: Boolean = false,
    val sendVarselbrev: Boolean = false,
) {
    fun <T> defaultWhenDisabled(toggle: Toggles.() -> Boolean, default: () -> T): T {
        if (toggle()) {
            error("Feature toggle er slått på, men mangler funsjonalitet")
        }
        return default()
    }
}
