package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse

object AvventerFagsysteminfo : Tilstand {
    override val navn: String = "AvventerFagsysteminfo"

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.eksternFagsak.trengerFagsysteminfo()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
    ) {
        tilbakekreving.byttTilstand(SendVarselbrev)
    }
}
