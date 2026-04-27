package no.nav.tilbakekreving.behandlingslogg

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnslag(
    override val id: UUID,
    val behandlingId: UUID?,
    val opprettetTid: LocalDateTime,
    val behandlingsloggstype: Behandlingsloggstype,
    val rolle: Rolle,
    val behandlerIdent: String,
    val brevRef: HistorikkReferanse<UUID, Brev>?,
) : Historikk.HistorikkInnslag<UUID> {
    fun tilEntity(tilbakekrevingId: String): LoggInnlagEntity = LoggInnlagEntity(
        id = id,
        tilbakekrevingRef = tilbakekrevingId,
        behandlingId = behandlingId,
        behandlingsloggstype = behandlingsloggstype,
        rolle = rolle,
        behandlerIdent = behandlerIdent,
        opprettetTid = opprettetTid,
        brevRef = brevRef?.tilEntity(),
    )
}

enum class Rolle {
    SAKSBEHANDLER,
    BESLUTTER,
    VEDTAKSLØSNING,
}
