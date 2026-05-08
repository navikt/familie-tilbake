package no.nav.tilbakekreving.fagsystem.events

import java.time.OffsetDateTime

data class BehandlingEndret(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: OffsetDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: Tilbakekreving,
) : Hendelse {
    override val versjon: Int = 1
}
