package no.nav.tilbakekreving.api.v2

data class OpprettTilbakekrevingEvent(
    val eksternFagsak: EksternFagsakDto,
    val opprettelsesvalg: Opprettelsevalg,
)
