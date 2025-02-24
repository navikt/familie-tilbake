package no.nav.familie.tilbake.kontrakter.klage

import no.nav.familie.tilbake.kontrakter.Regelverk
import java.time.LocalDateTime

data class FagsystemVedtak(
    val eksternBehandlingId: String,
    val behandlingstype: String,
    val resultat: String,
    val vedtakstidspunkt: LocalDateTime,
    val fagsystemType: FagsystemType,
    val regelverk: Regelverk?,
)

enum class FagsystemType {
    TILBAKEKREVING,
}
