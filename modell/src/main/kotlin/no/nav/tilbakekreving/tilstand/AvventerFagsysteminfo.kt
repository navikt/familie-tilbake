package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.saksbehandler.Behandler

object AvventerFagsysteminfo : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.eksternFagsak.trengerFagsysteminfo()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
        sporing: Sporing,
    ) {
        val eksternBehandling = tilbakekreving.eksternFagsak.lagre(fagsysteminfo)
        tilbakekreving.opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning)
        tilbakekreving.opprettBruker(fagsysteminfo.aktør)
        tilbakekreving.byttTilstand(AvventerBrukerinfo)
    }
}
