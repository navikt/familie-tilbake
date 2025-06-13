package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

internal sealed interface Tilstand {
    val tilbakekrevingTilstand: TilbakekrevingTilstand

    fun entering(tilbakekreving: Tilbakekreving)

    fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingHendelse,
    ) {
        error("Forventet ikke OpprettTilbakekrevingEvent i $tilbakekrevingTilstand")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        error("Forventet ikke Kravgrunnlag i $tilbakekrevingTilstand")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
    ) {
        error("Forventet ikke Fagsysteminfo i $tilbakekrevingTilstand")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
    ) {
        error("Forventet ikke Brukerinfo i $tilbakekrevingTilstand")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        error("Forventet ikke VarselbrevSendtHendelse i $tilbakekrevingTilstand")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        påminnelse: Påminnelse,
    ) {}

    fun håndterNullstilling(tilbakekreving: Tilbakekreving) {
        error("Forventet ikke Nullstilling i $tilbakekrevingTilstand")
    }
}
