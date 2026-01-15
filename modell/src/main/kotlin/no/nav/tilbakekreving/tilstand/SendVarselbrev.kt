package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object SendVarselbrev : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerVarselbrev()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.byttTilstand(TilBehandling)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        when (val brev = tilbakekreving.brevHistorikk.entry(varselbrevSendtHendelse.varselbrevId)) {
            is Varselbrev -> {
                brev.brevSendt(
                    journalpostId = varselbrevSendtHendelse.journalpostId!!,
                    tekstFraSaksbehandler = varselbrevSendtHendelse.tekstFraSaksbehandler,
                    sendtTid = varselbrevSendtHendelse.sendtTid,
                    fristForUttalelse = varselbrevSendtHendelse.fristForUttalelse,
                )
            }
            else -> error("Forventet Varselbrev for id=${varselbrevSendtHendelse.varselbrevId}, men var ${brev::class.simpleName}")
        }

        tilbakekreving.byttTilstand(TilBehandling)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, fagsysteminfo: FagsysteminfoHendelse) {
        tilbakekreving.oppdaterFagsysteminfo(fagsysteminfo)
    }
}
