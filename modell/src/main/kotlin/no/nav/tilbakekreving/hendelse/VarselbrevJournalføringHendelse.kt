package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevJournalføringHendelse(
    val varselbrevId: UUID,
    val journalpostId: String,
    val dokumentInfoId: String,
)
