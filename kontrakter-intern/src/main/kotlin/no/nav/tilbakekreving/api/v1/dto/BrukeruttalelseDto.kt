package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate

data class BrukeruttalelseDto(
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
    val uttalelsesdetaljer: List<Uttalelsesdetaljer>?,
    val utsettFrist: List<FristUtsettelse>?,
    val kommentar: String?,
)

data class Uttalelsesdetaljer(
    val uttalelsesdato: LocalDate,
    val hvorBrukerenUttalteSeg: String,
    val uttalelseBeskrivelse: String,
)

data class FristUtsettelse(
    val nyFrist: LocalDate,
    val begrunnelse: String,
)

enum class HarBrukerUttaltSeg {
    JA,
    NEI,
    UTTSETT_FRIST,
    ALLEREDE_UTTALET_SEG,
}
