package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object TilBehandling : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.TIL_BEHANDLING

    override fun entering(tilbakekreving: Tilbakekreving) {
        // TODO: Sende ut behov om saksbehandling
    }

    override fun håndterNullstilling(nåværendeBehandling: Behandling, sporing: Sporing) {
        nåværendeBehandling.flyttTilbakeTilFakta()
    }
}
