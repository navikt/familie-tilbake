package no.nav.tilbakekreving.config

import no.nav.tilbakekreving.Toggle
import java.util.EnumMap

data class Toggles(
    val revurdering: Boolean = false,
    val manuellOpprettelse: Boolean = false,
    val varselbrevEnabled: Boolean = false,
    val nyModell: EnumMap<Toggle, Boolean> = EnumMap(Toggle::class.java),
) {
    fun <T> defaultWhenDisabled(toggle: Toggles.() -> Boolean, default: () -> T): T {
        if (toggle()) {
            error("Feature toggle er slått på, men mangler funksjonalitet")
        }
        return default()
    }
}
