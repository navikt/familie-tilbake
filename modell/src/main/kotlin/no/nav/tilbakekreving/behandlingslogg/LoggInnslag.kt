package no.nav.tilbakekreving.behandlingslogg

import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnslag(
    override val id: UUID,
    val behandlingId: UUID?,
    val opprettetTid: LocalDateTime,
    val behandlingsloggstype: Behandlingsloggstype,
    val rolle: Rolle,
    val behandlerIdent: String,
) : Historikk.HistorikkInnslag<UUID> {
    fun tilEntity(tilbakekrevingId: String): LoggInnlagEntity = LoggInnlagEntity(
        id = id,
        tilbakekrevingRef = tilbakekrevingId,
        behandlingId = behandlingId,
        behandlingsloggstype = behandlingsloggstype,
        rolle = rolle,
        behandlerIdent = behandlerIdent,
        opprettetTid = opprettetTid,
    )
}

enum class Rolle {
    SAKSBEHANDLER,
    BESLUTTER,
    VEDTAKSLØSNING,
}
