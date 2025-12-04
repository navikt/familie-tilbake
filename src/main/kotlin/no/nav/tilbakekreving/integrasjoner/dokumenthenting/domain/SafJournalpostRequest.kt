package no.nav.tilbakekreving.integrasjoner.dokumenthenting.domain

data class SafJournalpostRequest(
    val variables: Any,
    val query: String,
)

data class SafRequestVariabler(
    val journalpostId: String,
)
