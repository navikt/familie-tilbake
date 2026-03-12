package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Utfører
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import java.time.LocalDateTime
import java.util.UUID

data class LoggInnlagEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val behandlingId: UUID,
    val opprettetTid: LocalDateTime,
    val type: Historikkinnslagstype,
    val utfører: Utfører,
    val utførerIdent: String,
    val tittel: String,
    val tekst: String? = null,
    val steg: String? = null,
) {
    fun fraEntity(): LoggInnslag = LoggInnslag(
        id = id,
        behandlingId = behandlingId,
        opprettetTid = opprettetTid,
        type = type,
        utfører = utfører,
        utførerIdent = utførerIdent,
        tittel = tittel,
        tekst = tekst,
        steg = steg,
    )
}
