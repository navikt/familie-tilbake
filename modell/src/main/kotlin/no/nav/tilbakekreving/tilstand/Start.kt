package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg

object Start : Tilstand {
    override val navn: String = "Start"

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingEvent,
    ) {
        when (hendelse.opprettelsesvalg) {
            Opprettelsesvalg.UTSETT_BEHANDLING_MED_VARSEL -> tilbakekreving.byttTilstand(AvventerUtsattBehandlingMedVarsel)
            Opprettelsesvalg.UTSETT_BEHANDLING_UTEN_VARSEL -> tilbakekreving.byttTilstand(AvventerUtsattBehandlingUtenVarsel)
            Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL -> tilbakekreving.byttTilstand(AvventerKravgrunnlag)
        }
    }
}
