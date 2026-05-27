package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object SendVarselbrev : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.TIL_FORHÅNDSVARSEL

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerVarselbrevJournalføring(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerVarselbrevJournalføring(sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevJournalføringHendelse: VarselbrevJournalføringHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.brevHistorikk.entry(varselbrevJournalføringHendelse.varselbrevId).brevSendt(
            journalpostId = varselbrevJournalføringHendelse.journalpostId,
            dokumentInfoId = varselbrevJournalføringHendelse.dokumentInfoId,
        )
        tilbakekreving.byttTilstand(DistribuerVarselbrev, sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, fagsysteminfo: FagsysteminfoHendelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.oppdaterFagsysteminfo(fagsysteminfo, sideeffektContext)
    }
}
