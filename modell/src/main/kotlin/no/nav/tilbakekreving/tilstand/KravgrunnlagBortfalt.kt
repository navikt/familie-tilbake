package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object KravgrunnlagBortfalt : Tilstand {
    override val tidTilPåminnelse: Duration = Duration.ofDays(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.KRAVGRUNNLAG_BORTFALT

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.TIL_BEHANDLING

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {}

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {}

    override fun validerInnenforScope(tilbakekreving: Tilbakekreving, toggles: FeatureToggles) {
        throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBortfalt, tilbakekreving.sporingsinformasjon())
    }
}
