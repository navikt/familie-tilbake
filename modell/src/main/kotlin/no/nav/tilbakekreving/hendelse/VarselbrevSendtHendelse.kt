package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevSendtHendelse(
    val varselbrevId: UUID,
    val behandlingId: UUID,
    val journalpostId: String,
    val behandlerIdent: String,
    val type: Hendelsestype,
)

enum class Hendelsestype {
    JOURNALFØRING,
    DISTRIBUERING,
}
