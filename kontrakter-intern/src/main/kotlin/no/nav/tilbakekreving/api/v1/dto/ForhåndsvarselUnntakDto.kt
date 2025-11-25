package no.nav.tilbakekreving.api.v1.dto

data class ForhåndsvarselUnntakDto(
    val begrunnelseForUnntak: VarslingsUnntak,
    val beskrivelse: String,
)

enum class VarslingsUnntak(val beskrivelse: String) {
    IKKE_PRAKTISK_MULIG(
        "Varsling er ikke praktisk mulig eller vil hindre gjennomføring av vedtaket.",
    ),

    UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING(
        "Mottaker av varselet har ukjent adresse og ettersporing er urimelig ressurskrevende.",
    ),

    ÅPENBART_UNØDVENDIG(
        "Varsel anses som åpenbart unødvendig eller mottaker er allerede kjent med saken og har hatt mulighet til å uttale seg.",
    ),
}
