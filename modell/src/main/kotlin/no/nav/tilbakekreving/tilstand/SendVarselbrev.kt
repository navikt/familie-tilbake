package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object SendVarselbrev : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.UTREDES

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerVarselbrevJournalføring()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.trengerVarselbrevJournalføring()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevJournalføringHendelse: VarselbrevJournalføringHendelse,
    ) {
        tilbakekreving.brevHistorikk.entry(varselbrevJournalføringHendelse.varselbrevId).brevSendt(journalpostId = varselbrevJournalføringHendelse.journalpostId)
        tilbakekreving.byttTilstand(DistribuerVarselbrev)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, fagsysteminfo: FagsysteminfoHendelse) {
        tilbakekreving.oppdaterFagsysteminfo(fagsysteminfo)
    }
}
