package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse

internal sealed interface Tilstand {
    val navn: String

    fun entering(tilbakekreving: Tilbakekreving)

    fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingEvent,
    ) {
        error("Forventet ikke OpprettTilbakekrevingEvent i $navn")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        error("Forventet ikke Kravgrunnlag i $navn")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
    ) {
        error("Forventet ikke Fagsysteminfo i $navn")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
    ) {
        error("Forventet ikke Brukerinfo i $navn")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        error("Forventet ikke VarselbrevSendtHendelse i $navn")
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        påminnelse: Påminnelse,
    ) {}
}
