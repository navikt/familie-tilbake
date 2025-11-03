package no.nav.tilbakekreving

import java.util.EnumMap

class FeatureToggles(
    private val overrides: EnumMap<Toggle, Boolean>,
) {
    operator fun get(toggle: Toggle): Boolean = overrides[toggle] ?: toggle.default
}

enum class Toggle(val default: Boolean) {
    SendVarselbrev(default = false),
}
