package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk

@Serializable
data class EksternFagsakBehandlingHistorikkEntity(
    val historikk: List<EksternFagsakBehandlingEntity>,
) {
    fun fraEntity(): EksternFagsakBehandlingHistorikk {
        return EksternFagsakBehandlingHistorikk(
            historikk = historikk.map { it.fraEntity() }.toMutableList(),
        )
    }
}
