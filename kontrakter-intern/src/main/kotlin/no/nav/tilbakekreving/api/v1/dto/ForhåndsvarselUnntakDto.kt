package no.nav.tilbakekreving.api.v1.dto

import java.util.UUID

data class ForhåndsvarselUnntakDto(
    val behandlingId: UUID?,
    val begrunnelseForUnntak: VarslingsUnntak,
    val beskrivelse: String,
    val uttalelsesdetaljer: List<Uttalelsesdetaljer>?,
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

    ALLEREDE_UTTALET_SEG(
        "Mottaker er allerede kjent med saken og uttalet seg.",
    ),
}
