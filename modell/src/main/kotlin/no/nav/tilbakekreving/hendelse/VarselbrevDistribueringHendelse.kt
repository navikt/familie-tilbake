package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevDistribueringHendelse(
    val behandlingId: UUID,
    val brevId: UUID,
    val behandlerIdent: String,
    val fagsakId: String,
    val journalpostId: String,
    val dokumentInfoId: String,
)
