package no.nav.tilbakekreving.historikk

import no.nav.tilbakekreving.feil.Sporing

interface Historikk<IdType, Innslag : Historikk.HistorikkInnslag<IdType>> {
    fun lagre(innslag: Innslag): HistorikkReferanse<IdType, Innslag>

    fun finn(id: IdType, sporing: Sporing): HistorikkReferanse<IdType, Innslag>

    fun entry(id: IdType): Innslag

    fun nåværende(): HistorikkReferanse<IdType, Innslag>

    interface HistorikkInnslag<IdType> {
        val internId: IdType
    }
}
