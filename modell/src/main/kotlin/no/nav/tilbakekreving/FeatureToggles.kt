package no.nav.tilbakekreving

import java.util.EnumMap

class FeatureToggles(
    private val overrides: EnumMap<Toggle, Boolean>,
) {
    operator fun get(toggle: Toggle): Boolean = overrides[toggle] ?: toggle.default
}

enum class Toggle(val default: Boolean) {
    SendVarselbrev(default = false),
    Arkivering(default = false),
    Brevutsending(default = false),
    Dokumenthenting(default = false),
    Journalposthenting(default = false),
    Norg2(default = true),
    EregServices(default = true),
}
