package no.nav.familie.tilbake.kontrakter.dokarkiv.v2

import no.nav.familie.tilbake.kontrakter.Språkkode

data class Førsteside(
    val språkkode: Språkkode = Språkkode.NB,
    val navSkjemaId: String,
    val overskriftstittel: String,
)
