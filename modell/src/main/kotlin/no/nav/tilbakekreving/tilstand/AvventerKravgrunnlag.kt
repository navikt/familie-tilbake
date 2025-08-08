package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.saksbehandler.Behandler

object AvventerKravgrunnlag : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
        sporing: Sporing,
    ) {
        tilbakekreving.kravgrunnlagHistorikk.lagre(kravgrunnlag)
        when (tilbakekreving.eksternFagsak.ytelse.integrererMotFagsystem()) {
            true -> tilbakekreving.byttTilstand(AvventerFagsysteminfo)
            else -> {
                val eksternBehandling = tilbakekreving.eksternFagsak.lagreTomBehandling(kravgrunnlag.fagsystemVedtaksdato)
                tilbakekreving.opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning)
                tilbakekreving.opprettBruker(kravgrunnlag.vedtakGjelder)
                tilbakekreving.byttTilstand(AvventerBrukerinfo)
            }
        }
    }
}
