package no.nav.tilbakekreving.aktør

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import java.time.LocalDate

data class Brukerinfo(
    val ident: String,
    val navn: String,
    val språkkode: Språkkode,
    val dødsdato: LocalDate?,
)
