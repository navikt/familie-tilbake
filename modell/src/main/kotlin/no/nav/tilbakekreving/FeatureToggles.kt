package no.nav.tilbakekreving

import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.EnumMap

class FeatureToggles(
    private val overrides: EnumMap<Toggle, Boolean>,
    private val fagsystemToggle: EnumMap<FagsystemDTO, EnumMap<FagsystemToggle, Boolean>>,
) {
    operator fun get(toggle: Toggle): Boolean = overrides[toggle] ?: toggle.default
}

enum class Toggle(val default: Boolean) {
    SendAutomatiskVarselbrev(default = false),
    Arkivering(default = false),
    Brevutsending(default = false),
    Dokumenthenting(default = false),
    Journalposthenting(default = false),
    Norg2(default = true),
    EregServices(default = false),
    EntraProxy(default = true),
    FjernUttalelsesfrist(default = false),
}

enum class FagsystemToggle(val default: Boolean) {
    FORHÅNDSVARSEL_BEHANDLNGSSTATUSER(default = false),
}
