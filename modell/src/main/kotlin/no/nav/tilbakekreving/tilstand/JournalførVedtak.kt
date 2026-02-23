package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object JournalførVedtak : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.JOURNALFØR_VEDTAK

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.JOURNALFØR_VEDTAK

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerJournalføring()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.trengerJournalføring()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        journalføringHendelse: JournalføringHendelse,
    ) {
        tilbakekreving.brevHistorikk.entry(journalføringHendelse.brevId).brevSendt(journalføringHendelse.journalpostId)
        tilbakekreving.byttTilstand(DistribuerVedtak)
    }
}
