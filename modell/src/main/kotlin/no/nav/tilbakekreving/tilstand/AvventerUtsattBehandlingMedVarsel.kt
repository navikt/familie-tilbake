package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving

object AvventerUtsattBehandlingMedVarsel : Tilstand {
    override val navn: String = "AvventerUtsattBehandlingMedVarsel"

    override fun entering(tilbakekreving: Tilbakekreving) {
    }
}
