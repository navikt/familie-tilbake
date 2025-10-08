package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import java.util.UUID

data class EksternFagsakEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val eksternId: String,
    val ytelseEntity: YtelseEntity,
    val behandlinger: List<EksternFagsakBehandlingEntity>,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): EksternFagsak {
        val eksternFagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(
            historikk = behandlinger.map { it.fraEntity() }.toMutableList(),
        )
        return EksternFagsak(
            id = id,
            eksternId = eksternId,
            ytelse = ytelseEntity.fraEntity(),
            behandlinger = eksternFagsakBehandlingHistorikk,
            behovObservatør = behovObservatør,
        )
    }
}
