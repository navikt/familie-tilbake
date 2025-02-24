package no.nav.familie.tilbake.kontrakter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Behandlingstema(
    @JsonValue val value: String,
) {
    Barnetrygd("ab0270"),
    BarnetrygdEØS("ab0058"),
    OrdinærBarnetrygd("ab0180"),
    UtvidetBarnetrygd("ab0096"),
    Skolepenger("ab0177"),
    Barnetilsyn("ab0028"),
    Overgangsstønad("ab0071"),
    Kontantstøtte("ab0084"),
    KontantstøtteEØS("ab0060"),
    Feilutbetaling("ab0006"),
    Tilbakebetaling("ab0007"), // Tilbakekreving
    Klage("ae0058"),
    NasjonalInstitusjon("ab0437"), // Barnetrygd institusjon
    ;

    companion object {
        private val behandlingstemaMap = values().associateBy(Behandlingstema::value) + values().associateBy { it.name }

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): Behandlingstema = behandlingstemaMap[value] ?: throw error("Fant ikke Behandlingstema for value=$value")
    }
}
