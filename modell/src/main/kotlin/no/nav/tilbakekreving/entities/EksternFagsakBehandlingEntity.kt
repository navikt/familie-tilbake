package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import java.time.LocalDate
import java.util.UUID

@Serializable
data class EksternFagsakBehandlingEntity(
    val internId: String,
    val eksternId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: String,
) {
    fun fraEntity(): EksternFagsakBehandling =
        EksternFagsakBehandling(
            internId = UUID.fromString(internId),
            eksternId = eksternId,
            revurderingsresultat = revurderingsresultat,
            revurderingsårsak = revurderingsårsak,
            begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
            revurderingsvedtaksdato = LocalDate.parse(revurderingsvedtaksdato),
        )
}
