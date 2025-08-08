package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
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
        sporing: Sporing,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke OpprettTilbakekrevingEvent i $tilbakekrevingTilstand", sporing)
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
        sporing: Sporing,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Kravgrunnlag i $tilbakekrevingTilstand", sporing)
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
        sporing: Sporing,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Fagsysteminfo i $tilbakekrevingTilstand", sporing)
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
        sporing: Sporing,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Brukerinfo i $tilbakekrevingTilstand", sporing)
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
        sporing: Sporing,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke VarselbrevSendtHendelse i $tilbakekrevingTilstand", sporing)
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        påminnelse: Påminnelse,
    ) {}

    fun håndter(
        tilbakekreving: Tilbakekreving,
        iverksettelseHendelse: IverksettelseHendelse,
        sporing: Sporing,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke IverksettelseHendelse i $tilbakekrevingTilstand", sporing)
    }

    fun håndterNullstilling(tilbakekreving: Tilbakekreving, sporing: Sporing) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Nullstilling i $tilbakekrevingTilstand", sporing)
    }
}
