package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse

object AvventerBrukerinfo : Tilstand {
    override val navn: String = "AvventerBrukerinfo"

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerBrukerinfo()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
    ) {
        tilbakekreving.bruker!!.oppdater(brukerinfo)
        tilbakekreving.opprettBrevmottakerSteg(brukerinfo.navn, brukerinfo.ident)
        tilbakekreving.byttTilstand(SendVarselbrev)
    }
}
