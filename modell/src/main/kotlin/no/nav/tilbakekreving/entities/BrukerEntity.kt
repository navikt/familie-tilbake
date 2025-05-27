package no.nav.tilbakekreving.entities

import java.time.LocalDate

data class BrukerEntity(
    val ident: String,
    var språkkode: String? = null,
    var navn: String? = null,
    var fødselsdato: LocalDate? = null,
    var kjønn: String? = null,
    var dødsdato: LocalDate? = null,
)
