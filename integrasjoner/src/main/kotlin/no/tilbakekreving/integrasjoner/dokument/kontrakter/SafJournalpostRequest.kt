package no.tilbakekreving.integrasjoner.dokument.kontrakter

data class SafJournalpostRequest(
    val variables: JournalposterForBrukerRequest,
    val query: String,
)

data class SafRequestVariabler(
    val journalpostId: String,
)
