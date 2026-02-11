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
    UNNTAK_ALLEREDE_UTTALET_SEG,
    UNNTAK_INGEN_UTTALELSE,
}
