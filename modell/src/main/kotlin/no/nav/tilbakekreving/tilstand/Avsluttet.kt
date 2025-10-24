package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object Avsluttet : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVSLUTTET

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.AVSLUTTET

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.sendStatusendring(ForenkletBehandlingsstatus.AVSLUTTET)
    }
}
