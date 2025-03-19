package no.nav.tilbakekreving.kontrakter.bruker

import java.time.LocalDate

data class FrontendBrukerDto(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn,
    val dødsdato: LocalDate? = null,
)
