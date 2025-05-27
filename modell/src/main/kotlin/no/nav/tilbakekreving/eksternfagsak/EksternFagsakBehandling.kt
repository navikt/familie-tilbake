package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDate
import java.util.UUID

class EksternFagsakBehandling(
    override val internId: UUID,
    internal val eksternId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
) : Historikk.HistorikkInnslag<UUID> {
    fun tilEntity(): EksternFagsakBehandlingEntity {
        return EksternFagsakBehandlingEntity(
            internId = internId,
            eksternId = eksternId,
            revurderingsresultat = revurderingsresultat,
            revurderingsårsak = revurderingsårsak,
            revurderingsvedtaksdato = revurderingsvedtaksdato,
            begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
        )
    }
}
