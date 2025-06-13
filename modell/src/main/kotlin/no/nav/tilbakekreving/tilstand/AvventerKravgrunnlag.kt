package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.feil.UtenforScopeException
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler

object AvventerKravgrunnlag : Tilstand {
    override val navn: String = "AvventerKravgrunnlag"

    override fun entering(tilbakekreving: Tilbakekreving) {}

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        tilbakekreving.kravgrunnlagHistorikk.lagre(kravgrunnlag)
        when (tilbakekreving.eksternFagsak.ytelse.integrererMotFagsystem()) {
            true -> tilbakekreving.byttTilstand(AvventerFagsysteminfo)
            else -> {
                val eksternBehandling = tilbakekreving.eksternFagsak.lagreTomBehandling(kravgrunnlag.fagsystemVedtaksdato)
                if (kravgrunnlag.vedtakGjelder !is KravgrunnlagHendelse.Aktør.Person) {
                    throw UtenforScopeException(UtenforScope.KravgrunnlagIkkePerson)
                }
                tilbakekreving.opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning)
                tilbakekreving.opprettBruker(kravgrunnlag.vedtakGjelder.ident)
                tilbakekreving.byttTilstand(AvventerBrukerinfo)
            }
        }
    }
}
