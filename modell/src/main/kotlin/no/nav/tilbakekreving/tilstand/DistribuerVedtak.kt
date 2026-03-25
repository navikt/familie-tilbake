package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object DistribuerVedtak : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.DISTRIUBER_VEDTAK

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.DISTRIUBER_VEDTAK

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerVedtaksbrevDistribusjon()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        if (tilbakekreving.features[Toggle.Vedtaksbrev]) {
            tilbakekreving.trengerVedtaksbrevDistribusjon()
        } else {
            tilbakekreving.byttTilstand(Avsluttet)
        }
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        distribusjonHendelse: DistribusjonHendelse,
    ) {
        tilbakekreving.byttTilstand(Avsluttet)
    }
}
