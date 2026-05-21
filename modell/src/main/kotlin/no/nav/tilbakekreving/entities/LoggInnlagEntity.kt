package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnlagEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val behandlingId: UUID?,
    val opprettetTid: LocalDateTime,
    val sistOppdatert: LocalDateTime?,
    val rolle: Rolle,
    val behandlerIdent: String,
    val behandlingsloggstype: Behandlingsloggstype,
    val ekstraInfo: Map<String, Any>?,
) {
    fun fraEntity(): LoggInnslag =
        LoggInnslag(
            id = id,
            behandlingId = behandlingId,
            opprettetTid = opprettetTid,
            sistOppdatert = sistOppdatert,
            behandlingsloggstype = behandlingsloggstype,
            rolle = rolle,
            behandlerIdent = behandlerIdent,
            ekstraInfo = ekstraInfo?.mapKeys { (key, _) ->
                EkstraInfo.valueOf(key)
            } ?: emptyMap(),
        )
}
