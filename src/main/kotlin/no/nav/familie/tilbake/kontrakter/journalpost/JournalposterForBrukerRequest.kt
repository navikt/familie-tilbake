package no.nav.familie.tilbake.kontrakter.journalpost

import no.nav.familie.tilbake.kontrakter.Tema

data class JournalposterForBrukerRequest(
    val brukerId: Bruker,
    val antall: Int,
    val tema: List<Tema>? = null,
    val journalposttype: List<Journalposttype>? = null,
)
