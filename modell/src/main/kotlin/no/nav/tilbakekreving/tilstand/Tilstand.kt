package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent

sealed interface Tilstand {
    val navn: String

    fun entering(tilbakekreving: Tilbakekreving)

    fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingEvent,
    ) {
        error("Forventet ikke OpprettTilbakekrevingEvent i $navn")
    }
}
