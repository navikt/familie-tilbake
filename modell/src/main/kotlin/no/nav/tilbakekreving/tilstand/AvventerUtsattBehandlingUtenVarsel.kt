package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object AvventerUtsattBehandlingUtenVarsel : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_UTSATT_BEHANDLING_UTEN_VARSEL

    override fun entering(tilbakekreving: Tilbakekreving) {
    }
}
