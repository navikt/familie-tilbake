package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.feil.Sporing
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
    val brevRef: HistorikkReferanseEntity<UUID>?,
) {
    fun fraEntity(brevHistorikk: BrevHistorikk, fagsakId: String): LoggInnslag =
        LoggInnslag(
            id = id,
            behandlingId = behandlingId,
            opprettetTid = opprettetTid,
            behandlingsloggstype = behandlingsloggstype,
            rolle = rolle,
            behandlerIdent = behandlerIdent,
            brevRef = brevRef?.let { brevHistorikk.finn(brevRef.id, Sporing(fagsakId, id.toString())) },
        )
}
