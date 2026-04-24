package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object DistribuerVarselbrev : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.DISTRIUBER_VARSELBREV

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.UTREDES

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerVarselbrevDistribusjon()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.trengerVarselbrevDistribusjon()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevJournalføringHendelse: VarselbrevDistribueringHendelse,
    ) {
        tilbakekreving.byttTilstand(TilBehandling)
    }
}
