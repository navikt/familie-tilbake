package no.nav.familie.tilbake.kontrakter.journalpost

import no.nav.tilbakekreving.kontrakter.BrukerIdType

data class Bruker(
    val id: String,
    val type: BrukerIdType,
)
