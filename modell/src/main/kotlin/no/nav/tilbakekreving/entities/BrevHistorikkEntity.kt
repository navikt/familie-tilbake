package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.BrevHistorikk

data class BrevHistorikkEntity(
    val historikk: List<BrevEntity>,
) {
    fun fraEntity(): BrevHistorikk {
        return BrevHistorikk(
            historikk = historikk.map { it.fraEntity() }.toMutableList(),
        )
    }
}
