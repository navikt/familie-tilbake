package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnlagEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val behandlingId: UUID?,
    val opprettetTid: LocalDateTime,
    val rolle: Rolle,
    val behandlerIdent: String,
    val behandlingsloggstype: Behandlingsloggstype,
) {
    fun fraEntity(): LoggInnslag = LoggInnslag(
        id = id,
        behandlingId = behandlingId,
        opprettetTid = opprettetTid,
        behandlingsloggstype = behandlingsloggstype,
        rolle = rolle,
        behandlerIdent = behandlerIdent,
    )
}
