package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class InfobrevDistribusjonHendelse(
    val infobrevId: UUID,
    val journalpostId: String,
    val dokumentInfoId: String,
)
