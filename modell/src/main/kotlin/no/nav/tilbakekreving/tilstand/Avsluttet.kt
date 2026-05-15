package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object Avsluttet : Tilstand {
    override val tidTilPåminnelse: Duration? = null
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVSLUTTET

    override fun behandlingsstatus(behandling: Behandling): BehandlingsstatusModell = BehandlingsstatusModell.AVSLUTTET

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.loggAvsluttning()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.oppdaterPåminnelsestidspunkt()
    }
}
