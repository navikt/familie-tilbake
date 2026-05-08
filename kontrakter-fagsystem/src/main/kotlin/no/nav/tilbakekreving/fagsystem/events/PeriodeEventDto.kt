package no.nav.tilbakekreving.fagsystem.events

import java.time.LocalDate

data class PeriodeEventDto(
    val fom: LocalDate,
    val tom: LocalDate,
)
