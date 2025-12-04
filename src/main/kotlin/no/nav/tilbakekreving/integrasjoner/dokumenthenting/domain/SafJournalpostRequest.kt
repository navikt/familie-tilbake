package no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain

import no.nav.familie.tilbake.kontrakter.journalpost.JournalposterForBrukerRequest

data class SafJournalpostRequest(
    val variables: JournalposterForBrukerRequest,
    val query: String,
)

data class SafRequestVariabler(
    val journalpostId: String,
)
