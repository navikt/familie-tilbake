package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving

object AvventerUtsattBehandlingUtenVarsel : Tilstand {
    override val navn: String = "AvventerUtsattBehandlingUtenVarsel"

    override fun entering(tilbakekreving: Tilbakekreving) {
    }
}
