package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object JournalførVedtak : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.JOURNALFØR_VEDTAK

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.JOURNALFØRER_VEDTAK

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.opprettVedtaksbrev(sideeffektContext.klokke)
        tilbakekreving.trengerVedtaksbrevJournalføring(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerVedtaksbrevJournalføring(sideeffektContext)
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
