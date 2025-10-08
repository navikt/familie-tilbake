package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class SendVarselbrevTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving i SendVarselbrev går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler())
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.håndter(fagsak)
        tilbakekreving.håndter(bruker)
        val varselbrevSendtHendelse = VarselbrevSendtHendelse(varselbrevId = tilbakekreving.brevHistorikk.nåværende().entry.id, journalpostId = "1234")
        tilbakekreving.håndter(varselbrevSendtHendelse)
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        tilbakekreving.tilstand shouldBe TilBehandling

        oppsamler.behovListe.forOne {
            val varselbrevBehov = it.shouldBeInstanceOf<VarselbrevBehov>()

            varselbrevBehov.brevId shouldBe varselbrevSendtHendelse.varselbrevId
            varselbrevBehov.brukerIdent shouldBe bruker.ident
            varselbrevBehov.brukerNavn shouldBe bruker.navn
            varselbrevBehov.språkkode shouldBe bruker.språkkode
            varselbrevBehov.varselbrev.id shouldBe varselbrevSendtHendelse.varselbrevId
            varselbrevBehov.varselbrev.kravgrunnlag.entry shouldBe kravgrunnlag
            varselbrevBehov.varselbrev.mottaker shouldBe behandling.brevmottakerSteg?.registrertBrevmottaker
            varselbrevBehov.varselbrev.ansvarligSaksbehandlerIdent shouldBe behandling.hentBehandlingsinformasjon().ansvarligSaksbehandler.ident
            varselbrevBehov.varselbrev.fristForTilbakemelding shouldBe LocalDate.now().plus(Period.ofWeeks(3))
            varselbrevBehov.feilutbetaltBeløp shouldBe kravgrunnlag.feilutbetaltBeløpForAllePerioder().toLong()
            varselbrevBehov.revurderingsvedtaksdato shouldBe fagsak.revurdering.vedtaksdato
            varselbrevBehov.varseltekstFraSaksbehandler shouldBe "Todo" // Hardkodet todo i koden også må fikses når vi vet mer
            varselbrevBehov.feilutbetaltePerioder shouldBe kravgrunnlag.datoperioder()
            varselbrevBehov.gjelderDødsfall shouldBe false
            varselbrevBehov.saksnummer shouldBe tilbakekreving.eksternFagsak.eksternId
            varselbrevBehov.ytelse shouldBe tilbakekreving.eksternFagsak.ytelse
            varselbrevBehov.behandlendeEnhet shouldBe tilbakekreving.behandlingHistorikk.nåværende().entry.hentBehandlingsinformasjon().enhet
        }
    }
}
