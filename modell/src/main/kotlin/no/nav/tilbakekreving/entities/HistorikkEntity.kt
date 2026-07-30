package no.nav.tilbakekreving.entities

import java.time.LocalDateTime

interface HistorikkInnslagEntity<IdType> {
    val id: IdType
    val opprettet: LocalDateTime
}

data class HistorikkEntity<IdType, InnslagEntity : HistorikkInnslagEntity<IdType>, InnslagType>(
    val innslag: List<InnslagEntity>,
) {
    fun fraEntity(map: (InnslagEntity) -> InnslagType) = innslag.sortedBy { it.opprettet }.map(map)
}
