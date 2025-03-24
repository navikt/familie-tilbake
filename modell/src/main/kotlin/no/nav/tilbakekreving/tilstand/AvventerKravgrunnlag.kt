package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse

object AvventerKravgrunnlag : Tilstand {
    override val navn: String = "AvventerKravgrunnlag"

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        tilbakekreving.byttTilstand(AvventerFagsysteminfo)
    }
}
