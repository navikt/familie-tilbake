package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

data class FagsysteminfoHendelse(
    val aktør: Aktør,
    val revurdering: Revurdering,
) {
    data class UtvidetPeriode(
        val kravgrunnlagPeriode: Datoperiode,
        val vedtakPeriode: Datoperiode,
    )

    data class Revurdering(
        val behandlingId: String,
        val årsak: EksternFagsakRevurdering.Revurderingsårsak,
        val årsakTilFeilutbetaling: String?,
        val vedtaksdato: LocalDate,
        val utvidPerioder: List<UtvidetPeriode>?,
    )
}
