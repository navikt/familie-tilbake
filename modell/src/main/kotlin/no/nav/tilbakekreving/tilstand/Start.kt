package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsevalg

object Start : Tilstand {
    override val navn: String = "Start"

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingEvent,
    ) {
        when (hendelse.opprettelsesvalg) {
            Opprettelsevalg.UTSETT_BEHANDLING_MED_VARSEL -> tilbakekreving.byttTilstand(AvventerUtsattBehandlingMedVarsel)
            Opprettelsevalg.UTSETT_BEHANDLING_UTEN_VARSEL -> tilbakekreving.byttTilstand(AvventerUtsattBehandlingUtenVarsel)
            Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL -> tilbakekreving.byttTilstand(AvventerKravgrunnlag)
        }
    }
}
