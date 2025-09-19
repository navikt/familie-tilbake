package no.nav.tilbakekreving.api.v2

import java.time.LocalDate

data class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)
