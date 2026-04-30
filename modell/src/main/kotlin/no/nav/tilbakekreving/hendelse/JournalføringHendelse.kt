package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class JournalføringHendelse(
    val brevId: UUID,
    val behandlingId: UUID,
    val journalpostId: String,
    val fagsakId: String,
    val dokumentInfoId: String,
)
