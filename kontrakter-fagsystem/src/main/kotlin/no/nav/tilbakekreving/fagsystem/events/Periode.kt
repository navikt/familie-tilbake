package no.nav.tilbakekreving.fagsystem.events

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)
