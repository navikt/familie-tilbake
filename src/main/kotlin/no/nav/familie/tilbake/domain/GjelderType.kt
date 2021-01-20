package no.nav.familie.tilbake.domain

enum class GjelderType(val navn: String) {

    PERSON("Person"),
    ORGANISASJON("Organisasjon"),
    SAMHANDLER("Samhandler"),
    APPLIKASJONSBRUKER("Applikasjonsbruker");

}