package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class DistribusjonHendelse(
    val behandlingId: UUID,
    val brevId: UUID,
    val fagsakId: String,
)
