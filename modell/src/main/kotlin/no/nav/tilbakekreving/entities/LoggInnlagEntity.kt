package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Utfører
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnlagEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val behandlingId: UUID?,
    val opprettetTid: LocalDateTime,
    val utfører: Utfører,
    val utførerIdent: String,
    val behandlingsloggstype: Behandlingsloggstype,
) {
    fun fraEntity(): LoggInnslag = LoggInnslag(
        id = id,
        behandlingId = behandlingId,
        opprettetTid = opprettetTid,
        behandlingsloggstype = behandlingsloggstype,
        utfører = utfører,
        utførerIdent = utførerIdent,
    )
}
