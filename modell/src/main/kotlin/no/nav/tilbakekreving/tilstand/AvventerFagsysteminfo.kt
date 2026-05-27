package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object AvventerFagsysteminfo : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(3)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.OPPRETTET

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerFagsysteminfo(sideeffektContext)
        if (!tilbakekreving.eksternFagsak.ytelse.integrererMotFagsystem()) {
            tilbakekreving.opprettBehandlingUtenIntegrasjon(sideeffektContext)
        }
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerFagsysteminfo(sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        val eksternBehandling = tilbakekreving.eksternFagsak.lagre(fagsysteminfo)
        tilbakekreving.opprettBehandling(eksternBehandling, sideeffektContext, fagsysteminfo.behandlendeEnhet)
        if (fagsysteminfo.aktør != null) {
            tilbakekreving.opprettBruker(fagsysteminfo.aktør)
        }
        tilbakekreving.byttTilstand(AvventerBrukerinfo, sideeffektContext)
    }
}
