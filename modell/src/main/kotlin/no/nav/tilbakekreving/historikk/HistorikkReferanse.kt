package no.nav.tilbakekreving.historikk

class HistorikkReferanse<IdType, Innslag : Historikk.HistorikkInnslag<IdType>>(
    private val historikk: Historikk<IdType, Innslag>,
    private val id: IdType,
) {
    val entry get() = historikk.finn(id)
}
