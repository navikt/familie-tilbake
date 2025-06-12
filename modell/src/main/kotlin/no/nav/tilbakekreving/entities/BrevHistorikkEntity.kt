package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.brev.BrevHistorikk

@Serializable
data class BrevHistorikkEntity(
    val historikk: List<BrevEntity>,
) {
    fun fraEntity(): BrevHistorikk {
        return BrevHistorikk(
            historikk = historikk.map { it.fraEntity() }.toMutableList(),
        )
    }
}
