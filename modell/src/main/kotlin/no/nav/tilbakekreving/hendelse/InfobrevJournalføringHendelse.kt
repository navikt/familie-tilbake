package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class InfobrevJournalføringHendelse(
    val infobrevId: UUID,
    val journalpostId: String,
    val dokumentInfoId: String,
)
