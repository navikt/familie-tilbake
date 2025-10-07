package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object SendVarselbrev : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun entering(tilbakekreving: Tilbakekreving) {
        val brukerBrevmetadata = tilbakekreving.bruker!!.hentBrevmetadata()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val fagsak = tilbakekreving.eksternFagsak
        val kravgrunnlag = tilbakekreving.kravgrunnlagHistorikk.nåværende().entry

        val varselbrev = Varselbrev.opprett(
            bruker = brukerBrevmetadata,
            mottaker = tilbakekreving.behandlingHistorikk.nåværende().entry.brevmottakerSteg!!.registrertBrevmottaker,
            behandling = behandling.hentBehandlingsinformasjon(),
            fagsak = fagsak,
            beløp = kravgrunnlag.feilutbetaltBeløpForAllePerioder(),
            feilutbetaltePerioder = kravgrunnlag.datoperioder(),
        )

        tilbakekreving.brevHistorikk.lagre(varselbrev)

        tilbakekreving.trengerVarselbrev(varselbrev as Varselbrev)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        when (val brev = tilbakekreving.brevHistorikk.entry(varselbrevSendtHendelse.varselbrevId)) {
            is Varselbrev -> brev.journalpostId = varselbrevSendtHendelse.journalpostId
            else -> error("Forventet Varselbrev for id=${varselbrevSendtHendelse.varselbrevId}, men var ${brev::class.simpleName}")
        }

        tilbakekreving.byttTilstand(TilBehandling)
    }
}
