package no.nav.tilbakekreving.api.v2

data class OpprettTilbakekrevingEvent(
    val eksternFagsak: EksternFagsakDto,
    val opprettelsesvalg: Opprettelsevalg,
    val revurderingsårsak: String,
    val revurderingsresultat: String,
    val varsletBeløp: Long? = null,
)
