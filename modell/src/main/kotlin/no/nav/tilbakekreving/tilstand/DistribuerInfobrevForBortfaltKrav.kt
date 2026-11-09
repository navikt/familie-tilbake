package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.InfobrevDistribusjonHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object DistribuerInfobrevForBortfaltKrav : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.DISTRIBUER_INFOBREV_FOR_BORTFALT_KRAV

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.TIL_BEHANDLING

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerInfobrevDistribusjon(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerInfobrevDistribusjon(sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        infobrevDistribusjonHendelse: InfobrevDistribusjonHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.byttTilstand(AvsluttetUtenVedtak, sideeffektContext)
    }

    override fun validerInnenforScope(tilbakekreving: Tilbakekreving) {
        throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBortfalt, tilbakekreving.sporingsinformasjon())
    }
}
