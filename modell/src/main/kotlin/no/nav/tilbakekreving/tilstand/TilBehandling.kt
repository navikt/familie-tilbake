package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object TilBehandling : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofDays(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.TIL_BEHANDLING
    override val kanEndresAvSaksbehandler: Boolean = true

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell {
        return behandling.førsteUfullstendigeSteg(klokke)
            ?.behandlingsstatus
            ?: BehandlingsstatusModell.TIL_BEHANDLING
    }

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {}

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        if (tilbakekreving.eksternFagsak.behandlinger.nåværende().entry is EksternFagsakRevurdering.Ukjent) {
            tilbakekreving.trengerFagsysteminfo(sideeffektContext)
        }
        tilbakekreving.påminnNåværendePeriode(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, fagsysteminfo: FagsysteminfoHendelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.oppdaterFagsysteminfo(fagsysteminfo, sideeffektContext)
    }

    override fun <T> gjørSaksbehandling(
        tilbakekreving: Tilbakekreving,
        behandling: Behandling,
        sideeffektContext: SideeffektContext,
        callback: (Behandling) -> T,
    ): T {
        return behandling.utførEndring(tilbakekreving::tilstand, sideeffektContext, tilbakekreving, tilbakekreving.eksternFagsak.ytelse) {
            callback(this).also {
                if (behandling.kanUtbetales(sideeffektContext.klokke)) {
                    tilbakekreving.byttTilstand(IverksettVedtak, sideeffektContext)
                }
            }
        }
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.hånterEndretKravgrunnlag(kravgrunnlag, sideeffektContext)
        tilbakekreving.trengerFagsysteminfo(sideeffektContext)
    }
}
