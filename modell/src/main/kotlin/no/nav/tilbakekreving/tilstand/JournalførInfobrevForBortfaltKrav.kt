package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object JournalførInfobrevForBortfaltKrav : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.JOURNALFØR_VEDTAK_FOR_BORTFALT_KRAV

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.JOURNALFØRER_VEDTAK

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBortfalt, tilbakekreving.sporingsinformasjon())
        // tilbakekreving.opprettInfobrev(sideeffektContext.klokke)
        // tilbakekreving.trengerInfobrevJournalføring(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBortfalt, tilbakekreving.sporingsinformasjon())
        // oppretting av infobrev må skje i entering, denne må fjernes når utenom scope exception er fjernet og alle behandlingene i denne tilstanden er påminnet.
        // tilbakekreving.opprettInfobrev(sideeffektContext.klokke)
        // tilbakekreving.trengerInfobrevJournalføring(sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        journalføringHendelse: JournalføringHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.brevHistorikk.entry(journalføringHendelse.brevId).brevSendt(
            journalpostId = journalføringHendelse.journalpostId,
            dokumentInfoId = journalføringHendelse.dokumentInfoId,
        )
        tilbakekreving.byttTilstand(DistribuerVedtak, sideeffektContext)
    }
}
