package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class JournalføringHendelse(
    val brevId: UUID,
    val journalpostId: String,
)
