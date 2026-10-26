package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.AvsluttetStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.EndretStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.SperretStatusmeldingHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object Sperret : Tilstand {
    override val tidTilPåminnelse: Duration? = null
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SPERRET
    override val erSperret: Boolean = true

    override fun validerInnenforScope(tilbakekreving: Tilbakekreving, toggles: FeatureToggles) {
        if (!toggles[Toggle.SperretStatusmelding]) {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagSperret, tilbakekreving.sporingsinformasjon())
        }
    }

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.TIL_BEHANDLING

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.hånterEndretKravgrunnlag(kravgrunnlag, sideeffektContext)
        tilbakekreving.trengerFagsysteminfo(sideeffektContext)
        tilbakekreving.byttTilstand(TilBehandling, sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: AvsluttetStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        if (sideeffektContext.features[Toggle.KravBortfalt]) {
            tilbakekreving.byttTilstand(JournalførInfobrevForBortfaltKrav, sideeffektContext)
            return
        }
        tilbakekreving.byttTilstand(KravgrunnlagBortfalt, sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, statusmeldingHendelse: EndretStatusmeldingHendelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.byttTilstand(TilBehandling, sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, statusmeldingHendelse: SperretStatusmeldingHendelse, sideeffektContext: SideeffektContext) {
    }

    override fun venter(klokke: Klokke, sporing: Sporing): Venter {
        return Venter(
            grunn = Venter.Grunn.SPERRET_VENTER_PÅ_OPPDATERING_FRA_ØKONOMI,
            frist = klokke.dagensDato().plusDays(1),
        )
    }
}
