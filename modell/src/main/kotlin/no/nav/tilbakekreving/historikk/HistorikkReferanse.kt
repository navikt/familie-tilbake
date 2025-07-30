package no.nav.tilbakekreving.historikk

import no.nav.tilbakekreving.entities.HistorikkReferanseEntity

class HistorikkReferanse<IdType, Innslag : Historikk.HistorikkInnslag<IdType>>(
    private val historikk: Historikk<IdType, Innslag>,
    private val id: IdType,
) {
    val entry get() = historikk.entry(id)

    fun tilEntity(): HistorikkReferanseEntity<IdType> = HistorikkReferanseEntity(id)
}
