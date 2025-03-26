package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse

object SendVarselbrev : Tilstand {
    override val navn = "SendVarselbrev"

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerVarselbrev()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        tilbakekreving.byttTilstand(TilBehandling)
    }
}
