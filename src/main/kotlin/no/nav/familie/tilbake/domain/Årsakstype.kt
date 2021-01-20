package no.nav.familie.tilbake.domain

enum class Årsakstype(val navn: String) {

    FEIL_FAKTA("Feil fakta"),
    FEIL_LOV("Feil lovanvendelse"),
    FEIL_REGEL("Feil regelforståelse"),
    ANNET("Annet"),
    UDEFINERT("Ikke definert")
}