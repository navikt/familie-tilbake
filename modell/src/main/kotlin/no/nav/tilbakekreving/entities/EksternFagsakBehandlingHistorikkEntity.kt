package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk

data class EksternFagsakBehandlingHistorikkEntity(
    val historikk: List<EksternFagsakBehandlingEntity>,
) {
    fun fraEntity(): EksternFagsakBehandlingHistorikk {
        return EksternFagsakBehandlingHistorikk(
            historikk = historikk.map { it.tilDomain() }.toMutableList(),
        )
    }
}
