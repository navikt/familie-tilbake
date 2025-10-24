package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand

object SendVarselbrev : Tilstand {
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.SEND_VARSELBREV

    override fun entering(tilbakekreving: Tilbakekreving) {
        val brukerBrevmetadata = tilbakekreving.bruker!!.hentBrevmetadata()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val fagsak = tilbakekreving.eksternFagsak
        val kravgrunnlag = tilbakekreving.kravgrunnlagHistorikk.nåværende()

        val revurderingsvedtaksdato = fagsak.behandlinger.nåværende().entry.vedtaksdato
        val varseltekstFraSaksbehandler = "Todo" // todo Kanskje vi skal ha en varselTekst i behandling?

        val varselbrev = Varselbrev.opprett(
            mottaker = behandling.brevmottakerSteg!!.registrertBrevmottaker,
            brevmottakerStegId = behandling.brevmottakerSteg!!.id,
            ansvarligSaksbehandlerIdent = behandling.hentBehandlingsinformasjon().ansvarligSaksbehandler.ident,
            kravgrunnlag = kravgrunnlag,
        )

        tilbakekreving.brevHistorikk.lagre(varselbrev)

        tilbakekreving.trengerVarselbrev(
            VarselbrevBehov(
                brevId = varselbrev.id,
                brukerIdent = brukerBrevmetadata.personIdent,
                brukerNavn = brukerBrevmetadata.navn,
                språkkode = brukerBrevmetadata.språkkode,
                varselbrev = varselbrev as Varselbrev,
                revurderingsvedtaksdato = revurderingsvedtaksdato,
                varseltekstFraSaksbehandler = varseltekstFraSaksbehandler,
                saksnummer = fagsak.eksternId,
                ytelse = fagsak.ytelse,
                behandlendeEnhet = behandling.hentBehandlingsinformasjon().enhet,
                feilutbetaltBeløp = varselbrev.hentVarsletBeløp(),
                feilutbetaltePerioder = kravgrunnlag.entry.datoperioder(),
                gjelderDødsfall = brukerBrevmetadata.dødsdato != null,
            ),
        )
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        varselbrevSendtHendelse: VarselbrevSendtHendelse,
    ) {
        when (val brev = tilbakekreving.brevHistorikk.entry(varselbrevSendtHendelse.varselbrevId)) {
            is Varselbrev -> {
                brev.journalpostId = varselbrevSendtHendelse.journalpostId
                // TODO: Oppdatere sendt når varselbrev faktisk blir sendt
            }
            else -> error("Forventet Varselbrev for id=${varselbrevSendtHendelse.varselbrevId}, men var ${brev::class.simpleName}")
        }

        tilbakekreving.byttTilstand(TilBehandling)
    }
}
