package no.nav.tilbakekreving.kontrakter

import java.time.LocalDate

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)
