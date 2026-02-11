package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate

data class BrukeruttalelseDto(
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
    val uttalelsesdetaljer: List<Uttalelsesdetaljer>?,
    val kommentar: String?,
)

data class Uttalelsesdetaljer(
    val uttalelsesdato: LocalDate,
    val hvorBrukerenUttalteSeg: String,
    val uttalelseBeskrivelse: String,
)

enum class HarBrukerUttaltSeg {
    JA_ETTER_FORHÅNDSVARSEL,
    NEI_ETTER_FORHÅNDSVARSEL,
    UNNTAK_ALLEREDE_UTTALT_SEG,
    UNNTAK_INGEN_UTTALELSE,

    @Deprecated("midreltidig, fjernes etter prodsatt og migrering")
    JA,

    @Deprecated("midreltidig, fjernes etter prodsatt og migrering")
    NEI,
}
