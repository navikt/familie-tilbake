package no.nav.tilbakekreving.historikk

interface Historikk<IdType, Innslag : Historikk.HistorikkInnslag<IdType>> {
    fun lagre(innslag: Innslag): HistorikkReferanse<IdType, Innslag>

    fun finn(id: IdType): Innslag

    fun nåværende(): HistorikkReferanse<IdType, Innslag>

    interface HistorikkInnslag<IdType> {
        val internId: IdType
    }
}
