package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import java.time.LocalDateTime

data class HistorikkinnslagDto(
    val behandlingId: String,
    val type: Historikkinnslagstype,
    val aktør: AktørDto,
    val aktørIdent: String,
    val tittel: String,
    val tekst: String? = null,
    val steg: String? = null,
    val journalpostId: String? = null,
    val dokumentId: String? = null,
    val opprettetTid: LocalDateTime,
) {
    enum class AktørDto {
        SAKSBEHANDLER,
        BESLUTTER,
        VEDTAKSLØSNING,
    }
}
