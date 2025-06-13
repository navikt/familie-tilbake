package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import java.time.LocalDate
import java.util.UUID

data class EksternFagsakBehandlingEntity(
    val type: EksternFagsakBehandlingType,
    val internId: String,
    val eksternId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: String,
    val revurderingsdatoFraKravgrunnlag: String? = null,
) {
    fun fraEntity(): EksternFagsakBehandling {
        return when (type) {
            EksternFagsakBehandlingType.BEHANDLING -> EksternFagsakBehandling.Behandling(
                internId = UUID.fromString(internId),
                eksternId = eksternId,
                revurderingsresultat = revurderingsresultat,
                revurderingsårsak = revurderingsårsak,
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                revurderingsvedtaksdato = LocalDate.parse(revurderingsvedtaksdato),
            )

            EksternFagsakBehandlingType.UKJENT -> EksternFagsakBehandling.Ukjent(
                internId = UUID.fromString(internId),
                revurderingsdatoFraKravgrunnlag = revurderingsdatoFraKravgrunnlag?.let { LocalDate.parse(it) },
            )
        }
    }
}

enum class EksternFagsakBehandlingType {
    BEHANDLING,
    UKJENT,
}
