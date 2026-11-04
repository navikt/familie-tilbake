package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.InfobrevJournalføringHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object JournalførInfobrevForBortfaltKrav : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.JOURNALFØR_INFOBREV_FOR_BORTFALT_KRAV

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.JOURNALFØRER_INFOBREV

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.opprettInfobrev(sideeffektContext.klokke)
        tilbakekreving.trengerInfobrevJournalføring(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerInfobrevJournalføring(sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        infobrevJournalføringHendelse: InfobrevJournalføringHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        tilbakekreving.brevHistorikk.entry(infobrevJournalføringHendelse.infobrevId).brevSendt(
            journalpostId = infobrevJournalføringHendelse.journalpostId,
            dokumentInfoId = infobrevJournalføringHendelse.dokumentInfoId,
        )
        tilbakekreving.byttTilstand(DistribuerInfobrevForBortfaltKrav, sideeffektContext)
    }
}
