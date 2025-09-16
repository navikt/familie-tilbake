package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object AvventerKravgrunnlag : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        tilbakekreving.kravgrunnlagHistorikk.lagre(kravgrunnlag)
        tilbakekreving.byttTilstand(AvventerFagsysteminfo)
    }
}
