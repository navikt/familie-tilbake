package no.nav.familie.tilbake.service.dokumentbestilling.brevmaler


enum class Dokumentmalstype(val kode: String, val navn: String) {
    INNHENT_DOKUMENTASJON("INNHEN", "Innhent dokumentasjon"),
    FRITEKSTBREV("FRITKS", "Fritekstbrev"),
    VARSEL("VARS", "Varsel om tilbakekreving"),
    KORRIGERT_VARSEL("KORRIGVARS", "Korrigert varsel om tilbakebetaling");
}