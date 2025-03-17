package no.nav.tilbakekreving.kontrakter.bruker

import java.time.LocalDate

data class BrukerDto(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate,
    val kjønn: Kjønn,
    val dødsdato: LocalDate? = null,
)
