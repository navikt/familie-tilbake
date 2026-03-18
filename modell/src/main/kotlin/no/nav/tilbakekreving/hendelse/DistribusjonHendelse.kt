package no.nav.tilbakekreving.hendelse

import java.util.UUID

data class DistribusjonHendelse(
    val bestillingsId: String,
    val behandlingId: UUID,
)
