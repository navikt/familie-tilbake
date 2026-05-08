package no.nav.tilbakekreving.fagsystem.events

import java.time.OffsetDateTime

data class BehandlingEndretEventDto(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: OffsetDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: TilbakekrevingEventDto,
) : HendelseEventDto {
    override val versjon: Int = 1
}
