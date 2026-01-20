package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object SendVarselbrev : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun entering(tilbakekreving: Tilbakekreving) {
        // Støttes ikke inntil videre. Entry er bak toggle for nå.
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.byttTilstand(TilBehandling)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        tilbakekreving.brevHistorikk.entry(varselbrevSendtHendelse.varselbrevId).brevSendt(journalpostId = varselbrevSendtHendelse.journalpostId!!)
        tilbakekreving.byttTilstand(TilBehandling)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, fagsysteminfo: FagsysteminfoHendelse) {
        tilbakekreving.oppdaterFagsysteminfo(fagsysteminfo)
    }
}
