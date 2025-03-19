package no.nav.tilbakekreving.api.v2

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

data class BrukerDto(
    val ident: String,
    val språkkode: Språkkode,
)
