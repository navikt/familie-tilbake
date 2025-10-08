package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevSendtHendelse(
    val varselbrevId: UUID,
    val journalpostId: String?,
)
