package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class VarselbrevDistribueringHendelse(
    val behandlingId: UUID,
    val behandlerIdent: String,
)
