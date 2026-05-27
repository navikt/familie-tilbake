package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

internal sealed interface Tilstand {
    val tidTilPåminnelse: Duration?
    val tilbakekrevingTilstand: TilbakekrevingTilstand
    val kanEndresAvSaksbehandler: Boolean get() = false

    fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell

    fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext)

    fun håndter(
        tilbakekreving: Tilbakekreving,
        hendelse: OpprettTilbakekrevingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke OpprettTilbakekrevingEvent i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        kravgrunnlag: KravgrunnlagHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Kravgrunnlag i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Fagsysteminfo i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        brukerinfo: BrukerinfoHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke Brukerinfo i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevJournalføringHendelse: VarselbrevJournalføringHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke journalføring av varselbrev i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevDistribueringHendelse: VarselbrevDistribueringHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke distribuering av varselbrev i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        påminnelse: Påminnelse,
        sideeffektContext: SideeffektContext,
    )

    fun håndter(
        tilbakekreving: Tilbakekreving,
        behandling: Behandling,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke totrinn vurdering i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        iverksettelseHendelse: IverksettelseHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke IverksettelseHendelse i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        journalføringHendelse: JournalføringHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke JournalføringHendelse i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndter(
        tilbakekreving: Tilbakekreving,
        distribusjonHendelse: DistribusjonHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        throw ModellFeil.UgyldigOperasjonException("Forventet ikke DistribusjonHendelse i $tilbakekrevingTilstand", tilbakekreving.sporingsinformasjon())
    }

    fun håndterNullstilling(nåværendeBehandling: Behandling, sporing: Sporing, sideeffektContext: SideeffektContext) {
        throw ModellFeil.UgyldigOperasjonException("Kan ikke flytte tilbake til fakta i $tilbakekrevingTilstand", sporing)
    }

    fun håndterTrekkTilbakeFraGodkjenning(behandling: Behandling, sporing: Sporing, sideeffektContext: SideeffektContext) {
        throw ModellFeil.UgyldigOperasjonException("Kan ikke trekke tilbake fra godkjenning $tilbakekrevingTilstand", sporing)
    }
}
