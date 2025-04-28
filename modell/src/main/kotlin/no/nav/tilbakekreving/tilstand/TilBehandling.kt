package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving

object TilBehandling : Tilstand {
    override val navn: String = "TilBehandling"

    override fun entering(tilbakekreving: Tilbakekreving) {
        // TODO: Sende ut behov om saksbehandling
    }

    override fun håndterNullstilling(tilbakekreving: Tilbakekreving) {
        tilbakekreving.nullstillBehandling()
    }
}
