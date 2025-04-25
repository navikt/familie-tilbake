package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving

object IverksettVedtak : Tilstand {
    override val navn: String = "IverksettVedtak"

    override fun entering(tilbakekreving: Tilbakekreving) {}
}
