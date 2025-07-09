package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.math.BigInteger
import java.util.UUID

object IverksettVedtak : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.IVERKSETT_VEDTAK

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerIverksettelse()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        iverksattVedtakId: UUID,
        vedtakId: BigInteger,
    ) {
        tilbakekreving.byttTilstand(Avsluttet)
    }
}
