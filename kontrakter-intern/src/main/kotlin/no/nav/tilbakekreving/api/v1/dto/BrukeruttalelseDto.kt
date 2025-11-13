package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate

data class BrukeruttalelseDto(
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
    val uttalelsesdetaljer: List<Uttalelsesdetaljer>?,
    val utsettFrist: LocalDate?,
    val beskrivelseVedNeiEllerUtsettFrist: String?,
)

data class Uttalelsesdetaljer(
    val uttalelsesdato: LocalDate?,
    val hvorBrukerenUttalteSeg: String?,
    val uttalelseBeskrivelse: String?,
)

enum class HarBrukerUttaltSeg {
    JA,
    NEI,
    UTTSETT_FRIST,
}
