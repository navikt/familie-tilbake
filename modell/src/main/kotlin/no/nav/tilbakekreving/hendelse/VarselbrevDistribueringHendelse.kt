package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevDistribueringHendelse(
    val brevId: UUID,
    val journalpostId: String,
    val dokumentInfoId: String,
)
