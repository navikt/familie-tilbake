package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler

object AvventerFagsysteminfo : Tilstand {
    override val navn: String = "AvventerFagsysteminfo"

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.eksternFagsak.trengerFagsysteminfo()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
    ) {
        val eksternBehandling = tilbakekreving.eksternFagsak.lagre(fagsysteminfo)
        tilbakekreving.opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning)
        tilbakekreving.opprettBruker(fagsysteminfo.ident)
        tilbakekreving.byttTilstand(AvventerBrukerinfo)
    }
}
