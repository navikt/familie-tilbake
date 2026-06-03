package no.nav.tilbakekreving.tilstand
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.distribusjon
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.iverksettelse
import no.nav.tilbakekreving.journalføring
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class DistribuerVedtaTest {
    @Test
    fun `tilbakekreving skall være i DistribuerVedtak tilstand når JournalførVedtak er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        val brevId = (oppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId
        tilbakekreving.håndter(
            JournalføringHendelse(
                brevId = brevId,
                journalpostId = "123",
                dokumentInfoId = "321",
                behandlingId = UUID.randomUUID(),
                fagsakId = tilbakekreving.eksternFagsak.eksternId,
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe DistribuerVedtak
    }

    @Test
    fun `tilbakekreving skall være i Avsluttet tilstand når DistribuerVedtak er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        val brevId = (oppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId
        tilbakekreving.håndter(
            journalføring(brevId, tilbakekreving.eksternFagsak.eksternId),
            systemContext(),
        )
        tilbakekreving.håndter(
            distribusjon(
                brevId = brevId,
                fagsakId = tilbakekreving.eksternFagsak.eksternId,
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe Avsluttet
    }

    @Test
    fun `tilbakekreving sender distribuerBehov på nytt`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        val brevId = (oppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId
        tilbakekreving.håndter(
            journalføring(
                brevId,
                fagsakId = tilbakekreving.eksternFagsak.eksternId,
            ),
            systemContext(behovObservatør = oppsamler),
        )

        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()), systemContext(behovObservatør = oppsamler))
        val vedtaksbrevBehov = oppsamler.behovListe.filterIsInstance<VedtaksbrevDistribusjonBehov>()

        vedtaksbrevBehov.shouldHaveSize(2)
        vedtaksbrevBehov.map { it.journalpostId }.distinct().shouldHaveSize(1)
    }

    private fun tilbakekrevingKlarTilJournalføring(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        oppsamler: BehovObservatørOppsamler,
        endringOppsamler: EndringObservatørOppsamler = EndringObservatørOppsamler(),
    ): Tilbakekreving {
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(endringOppsamler, behovObservatør = oppsamler),
        )
        tilbakekreving.apply {
            håndter(kravgrunnlag(), systemContext(endringOppsamler))
            håndter(fagsysteminfoHendelse(), systemContext(endringOppsamler))
            håndter(brukerinfoHendelse(), systemContext(endringOppsamler))
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext(endringOppsamler)) {
                lagreUttalelse(UttalelseVurdering.JA, null, "", saksbehandlerContext(endringOppsamler))
                håndter(saksbehandlerContext(endringOppsamler), faktastegVurdering())
                håndter(saksbehandlerContext(endringOppsamler), periode = 1.januar(2021) til 31.januar(2021), vurdering = foreldelseVurdering())
                håndter(saksbehandlerContext(endringOppsamler), periode = 1.januar(2021) til 31.januar(2021), vurdering = forårsaketAvBruker().uaktsomt())
                håndterForeslåVedtak(saksbehandlerContext(endringOppsamler))
            }
            gjørSaksbehandling(nåværendeBehandlingId(), beslutterContext(endringOppsamler)) {
                håndter(beslutterContext(endringOppsamler), godkjenning())
            }
            tilbakekreving.håndter(
                iverksettelse(),
                systemContext(endringOppsamler, behovObservatør = oppsamler),
            )
        }
        return tilbakekreving
    }
}
