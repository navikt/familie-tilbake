package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object AvventerBrukerinfo : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_BRUKERINFO

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerBrukerinfo()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.trengerBrukerinfo()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
    ) {
        tilbakekreving.bruker!!.oppdater(brukerinfo)
        if (tilbakekreving.features[Toggle.SendAutomatiskVarselbrev]) {
            /*Todo
                Vi må vurdere hvordan tekst fra saksbehandler skal hentes her når Automatisk varselbrev er på plass
             */
            tilbakekreving.trengerVarselbrev("To do")
        } else {
            tilbakekreving.byttTilstand(TilBehandling)
        }
    }
}
