package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object AvventerKravgrunnlag : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofDays(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        tilbakekreving.kravgrunnlagHistorikk.lagre(kravgrunnlag)
        tilbakekreving.byttTilstand(AvventerFagsysteminfo)
    }
}
