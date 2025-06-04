package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import java.time.LocalDate
import java.util.UUID

data class EksternFagsakBehandlingEntity(
    val internId: UUID,
    val eksternId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
) {
    fun tilDomain(): EksternFagsakBehandling =
        EksternFagsakBehandling(
            internId = internId,
            eksternId = eksternId,
            revurderingsresultat = revurderingsresultat,
            revurderingsårsak = revurderingsårsak,
            begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
            revurderingsvedtaksdato = revurderingsvedtaksdato,
        )
}
