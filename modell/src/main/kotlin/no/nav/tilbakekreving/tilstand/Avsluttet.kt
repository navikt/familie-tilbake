package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object Avsluttet : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVSLUTTET

    override fun entering(tilbakekreving: Tilbakekreving) {}
}
