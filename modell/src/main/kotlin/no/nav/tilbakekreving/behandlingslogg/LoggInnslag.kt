package no.nav.tilbakekreving.behandlingslogg

import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnslag(
    override val id: UUID,
    val behandlingId: UUID,
    val opprettetTid: LocalDateTime,
    val type: Historikkinnslagstype,
    val utfører: Utfører,
    val utførerIdent: String,
    val tittel: String,
    val tekst: String? = null,
    val steg: String? = null,
) : Historikk.HistorikkInnslag<UUID> {
    fun tilEntity(tilbakekrevingId: String): LoggInnlagEntity = LoggInnlagEntity(
        id = id,
        tilbakekrevingRef = tilbakekrevingId,
        behandlingId = behandlingId,
        type = type,
        utfører = utfører,
        utførerIdent = utførerIdent,
        tittel = tittel,
        tekst = tekst,
        steg = steg,
        opprettetTid = opprettetTid,
    )
}

enum class Utfører {
    SAKSBEHANDLER,
    BESLUTTER,
    VEDTAKSLØSNING,
}
