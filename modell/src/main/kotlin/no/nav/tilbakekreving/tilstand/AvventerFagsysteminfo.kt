package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.time.Duration

object AvventerFagsysteminfo : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(3)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerFagsysteminfo()
        if (!tilbakekreving.eksternFagsak.ytelse.integrererMotFagsystem()) {
            tilbakekreving.opprettBehandlingUtenIntegrasjon()
        }
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.trengerFagsysteminfo()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
    ) {
        val eksternBehandling = tilbakekreving.eksternFagsak.lagre(fagsysteminfo)
        tilbakekreving.opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning)
        tilbakekreving.opprettBruker(fagsysteminfo.aktør)
        tilbakekreving.byttTilstand(AvventerBrukerinfo)
    }
}
