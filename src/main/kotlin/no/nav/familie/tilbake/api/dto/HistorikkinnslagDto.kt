package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.Historikkinnslag
import no.nav.familie.tilbake.historikkinnslag.Historikkinnslagstype
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

fun Historikkinnslag.tilDto() =
    HistorikkinnslagDto(
        behandlingId = behandlingId.toString(),
        type = type,
        aktør =
            when (aktør) {
                Historikkinnslag.Aktør.BESLUTTER -> HistorikkinnslagDto.AktørDto.BESLUTTER
                Historikkinnslag.Aktør.SAKSBEHANDLER -> HistorikkinnslagDto.AktørDto.SAKSBEHANDLER
                Historikkinnslag.Aktør.VEDTAKSLØSNING -> HistorikkinnslagDto.AktørDto.VEDTAKSLØSNING
            },
        aktørIdent = opprettetAv,
        tittel = tittel,
        tekst = tekst,
        steg = steg,
        journalpostId = journalpostId,
        dokumentId = dokumentId,
        opprettetTid = opprettetTid,
    )
