package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.frontend.models.TilbakekrevingRevurderingsarsakDto
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration
import java.util.UUID

object Avsluttet : Tilstand {
    override val tidTilPåminnelse: Duration? = null
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVSLUTTET
    override val kanRevurderes = true

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.AVSLUTTET

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.loggAvsluttning(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        if (tilbakekreving.eksternFagsak.ytelse.brukerEksternFagsakIdForUrl) {
            tilbakekreving.påminnNåværendePeriode(sideeffektContext)
        }
    }

    override fun opprettRevurdering(
        tilbakekreving: Tilbakekreving,
        sideeffektContext: SideeffektContext,
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        behandlendeEnhet: String?,
        revurderingsårsak: TilbakekrevingRevurderingsarsakDto,
    ) {
        tilbakekreving.klonBehandling(revurderingsårsak, sideeffektContext)
        tilbakekreving.byttTilstand(TilBehandling, sideeffektContext)
    }
}
