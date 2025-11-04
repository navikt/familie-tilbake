package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

internal sealed interface Tilstand {
    val tidTilPåminnelse: Duration?
    val tilbakekrevingTilstand: TilbakekrevingTilstand

    fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.OPPRETTET

    fun entering(tilbakekreving: Tilbakekreving)

    fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingHendelse,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke OpprettTilbakekrevingEvent i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Kravgrunnlag i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Fagsysteminfo i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Brukerinfo i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke VarselbrevSendtHendelse i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        påminnelse: Påminnelse,
    )

    fun håndter(
        tilbakekreving: Tilbakekreving,
        iverksettelseHendelse: IverksettelseHendelse,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke IverksettelseHendelse i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndterNullstilling(nåværendeBehandling: Behandling, sporing: Sporing) {
        throw ModellFeil.UgyldigOperasjonException("Kan ikke flytte tilbake til fakta i $tilbakekrevingTilstand", sporing)
    }

    fun håndterTrekkTilbakeFraGodkjenning(behandling: Behandling, sporing: Sporing) {
        throw ModellFeil.UgyldigOperasjonException("Kan ikke trekke tilbake fra godkjenning $tilbakekrevingTilstand", sporing)
    }
}
