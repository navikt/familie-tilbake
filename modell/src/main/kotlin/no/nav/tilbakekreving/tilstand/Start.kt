package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object Start : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.START

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingHendelse,
    ) {
        when (hendelse.opprettelsesvalg) {
            Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL -> tilbakekreving.byttTilstand(AvventerKravgrunnlag)
        }
    }
}
