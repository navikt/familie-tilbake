package no.nav.familie.tilbake.kontrakter.journalpost

import no.nav.familie.tilbake.kontrakter.BrukerIdType

data class Bruker(
    val id: String,
    val type: BrukerIdType,
)
