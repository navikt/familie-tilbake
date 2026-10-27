package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.AvsluttetStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.EndretStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.SperretStatusmeldingHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object AvventerKravgrunnlag : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofDays(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.OPPRETTET

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {}

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.kravgrunnlagHistorikk.lagre(kravgrunnlag)
        tilbakekreving.byttTilstand(AvventerFagsysteminfo, sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: AvsluttetStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.byttTilstand(KravgrunnlagBortfalt, sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: SperretStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: EndretStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
    }
}
