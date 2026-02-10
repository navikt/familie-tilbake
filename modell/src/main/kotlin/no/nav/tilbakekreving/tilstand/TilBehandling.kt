package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object TilBehandling : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofDays(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.TIL_BEHANDLING

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus {
        return behandling.steg().firstOrNull { !it.erFullstendig() }
            ?.behandlingsstatus
            ?: Behandlingsstatus.UTREDES
    }

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.sendStatusendring(ForenkletBehandlingsstatus.TIL_BEHANDLING)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        if (tilbakekreving.eksternFagsak.behandlinger.nåværende().entry is EksternFagsakRevurdering.Ukjent) {
            tilbakekreving.trengerFagsysteminfo()
        }
        tilbakekreving.sendStatusendring(ForenkletBehandlingsstatus.TIL_BEHANDLING)
    }

    override fun håndterNullstilling(nåværendeBehandling: Behandling, sporing: Sporing) {
        nåværendeBehandling.flyttTilbakeTilFakta()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, fagsysteminfo: FagsysteminfoHendelse) {
        tilbakekreving.oppdaterFagsysteminfo(fagsysteminfo)
    }

    override fun håndterTrekkTilbakeFraGodkjenning(behandling: Behandling, sporing: Sporing) {
        behandling.trekkTilbakeFraGodkjenning()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        tilbakekreving.brevHistorikk.entry(varselbrevSendtHendelse.varselbrevId).brevSendt(journalpostId = varselbrevSendtHendelse.journalpostId!!)
    }
}
