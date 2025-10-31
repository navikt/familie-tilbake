package no.nav.tilbakekreving.hendelse

import java.time.LocalDateTime
import java.util.UUID

data class VarselbrevSendtHendelse(
    val varselbrevId: UUID,
    val journalpostId: String?,
    val sendtTid: LocalDateTime,
)
