package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object AvventerUtsattBehandlingMedVarsel : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_UTSATT_BEHANDLING_MED_VARSEL

    override fun entering(tilbakekreving: Tilbakekreving) {
    }
}
