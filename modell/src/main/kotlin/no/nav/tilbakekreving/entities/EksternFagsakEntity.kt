package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import java.util.UUID

data class EksternFagsakEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val eksternId: String,
    val ytelseEntity: YtelseEntity,
    val behandlinger: HistorikkEntity<UUID, EksternFagsakBehandlingEntity, EksternFagsakRevurdering>,
) {
    fun fraEntity(): EksternFagsak {
        val eksternFagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(
            historikk = behandlinger.fraEntity { it.fraEntity() }.toMutableList(),
        )
        return EksternFagsak(
            id = id,
            eksternId = eksternId,
            ytelse = ytelseEntity.fraEntity(),
            behandlinger = eksternFagsakBehandlingHistorikk,
        )
    }
}
