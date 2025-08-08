package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object SendVarselbrev : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.brevHistorikk.lagre(
            Varselbrev.opprett(tilbakekreving.kravgrunnlagHistorikk.nåværende().entry.feilutbetaltBeløpForAllePerioder().toLong()),
        )
        tilbakekreving.trengerVarselbrev()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
        sporing: Sporing,
    ) {
        tilbakekreving.byttTilstand(TilBehandling)
    }
}
