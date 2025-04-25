package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import java.time.LocalDate

data class BrukerinfoHendelse(
    val ident: String,
    val navn: String,
    val fødselsdato: LocalDate,
    val kjønn: Kjønn,
    val dødsdato: LocalDate? = null,
    val språkkode: Språkkode? = null,
)
