package no.nav.tilbakekreving.vedtak

import no.nav.familie.tilbake.common.repository.Sporbar
import java.util.UUID

data class IverksettRequest(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val melding: String,
    val alvorlighetsgrad: String?,
    val sporbar: Sporbar = Sporbar(),
)
