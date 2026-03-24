package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevJournalføringHendelse(
    val varselbrevId: UUID,
    val behandlingId: UUID,
    val journalpostId: String,
    val behandlerIdent: String,
)
