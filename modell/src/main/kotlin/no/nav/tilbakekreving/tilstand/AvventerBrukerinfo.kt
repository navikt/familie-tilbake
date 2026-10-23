package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.StatusmeldingHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object AvventerBrukerinfo : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_BRUKERINFO

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.OPPRETTET

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerBrukerinfo(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerBrukerinfo(sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.bruker!!.oppdater(brukerinfo)

        val sisteKravHendelse = tilbakekreving.kravgrunnlagHistorikk.nåværende().entry
        if (sisteKravHendelse is StatusmeldingHendelse && sisteKravHendelse.erKravgrunnlagSperret()) {
            tilbakekreving.byttTilstand(Sperret, sideeffektContext)
            return
        }
        // todo her kan vi bytte til SendVarselbrev tilstand når Automatisk varselbrev er på plass
        tilbakekreving.byttTilstand(TilBehandling, sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: StatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        if (statusmeldingHendelse.kravgrunnlagBortfalt()) {
            tilbakekreving.byttTilstand(KravgrunnlagBortfalt, sideeffektContext)
        }
    }
}
