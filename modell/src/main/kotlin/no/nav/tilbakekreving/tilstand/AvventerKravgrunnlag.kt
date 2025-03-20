package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving

object AvventerKravgrunnlag : Tilstand {
    override val navn: String = "AvventerKravgrunnlag"

    override fun entering(tilbakekreving: Tilbakekreving) {}
}
