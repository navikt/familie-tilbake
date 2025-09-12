package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

data class FagsysteminfoHendelse(
    val aktør: Aktør,
    val behandlingId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
    val utvidPerioder: List<UtvidetPeriode>?,
) {
    data class UtvidetPeriode(
        val kravgrunnlagPeriode: Datoperiode,
        val vedtakPeriode: Datoperiode,
    )
}
