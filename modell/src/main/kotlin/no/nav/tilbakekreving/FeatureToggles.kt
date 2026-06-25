package no.nav.tilbakekreving

import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.util.EnumMap

class FeatureToggles(
    private val overrides: EnumMap<Toggle, Boolean>,
    private val fagsystemToggles: EnumMap<FagsystemDTO, EnumMap<FagsystemToggle, Boolean>>,
) {
    operator fun get(toggle: Toggle): Boolean = overrides[toggle] ?: toggle.default

    operator fun get(ytelse: Ytelse, fagsystemToggle: FagsystemToggle): Boolean = fagsystemToggles[ytelse.tilFagsystemDTO()]?.get(fagsystemToggle) ?: fagsystemToggle.default
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
    OppdragRestClient(default = false),
}

enum class FagsystemToggle(val default: Boolean) {
    ForhaandsvarselBehandlingsstatuser(default = false),
}
