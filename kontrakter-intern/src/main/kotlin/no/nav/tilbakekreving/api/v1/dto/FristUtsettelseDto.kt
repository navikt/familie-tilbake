package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate

class FristUtsettelseDto(
    val nyFrist: LocalDate,
    val begrunnelse: String,
)
