package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving

object SendVarselbrev : Tilstand {
    override val navn = "SendVarselbrev"

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerVarselbrev()
    }
}
