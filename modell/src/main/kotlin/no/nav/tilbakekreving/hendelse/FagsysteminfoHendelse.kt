package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate

data class FagsysteminfoHendelse(
    val aktør: Aktør?,
    val revurdering: Revurdering,
    val utvidPerioder: List<UtvidetPeriode>?,
) {
    data class UtvidetPeriode(
        val kravgrunnlagPeriode: Datoperiode,
        val vedtaksperiode: Datoperiode,
    )

    data class Revurdering(
        val behandlingId: String,
        val årsak: EksternFagsakRevurdering.Revurderingsårsak,
        val årsakTilFeilutbetaling: String?,
        val vedtaksdato: LocalDate,
    )
}
