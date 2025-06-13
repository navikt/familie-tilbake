package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.fagsystem.Ytelse

class OpprettTilbakekrevingHendelse(
    val opprettelsesvalg: Opprettelsesvalg,
    val eksternFagsak: EksternFagsak,
) {
    class EksternFagsak(
        val eksternId: String,
        val ytelse: Ytelse,
    )
}
