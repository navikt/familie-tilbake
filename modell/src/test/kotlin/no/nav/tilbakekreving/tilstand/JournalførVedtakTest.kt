package no.nav.tilbakekreving.tilstand
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
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
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class JournalførVedtakTest {
    @Test
    fun `tilbakekreving skal være i journalføring tilstand og vedtaksbrev opprettes når iverksettelse er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        tilbakekreving.håndter(
            iverksettelse(),
            systemContext(behovObservatør = oppsamler),
        )
        val brev = tilbakekreving.brevHistorikk.nåværende().entry
        brev.shouldBe(
            Vedtaksbrev(
                id = oppsamler.behovListe.filterIsInstance<VedtaksbrevJournalføringBehov>().first().brevId,
                journalpostId = null,
                dokumentInfoId = null,
                sendtTid = LocalDate.now(),
            ),
        )
        brev.id shouldBe oppsamler.behovListe.filterIsInstance<VedtaksbrevJournalføringBehov>().first().brevId
        brev.journalpostId shouldBe null
        tilbakekreving.tilstand shouldBe JournalførVedtak
    }

    @Test
    fun `tilbakekreving sender journalføringBehov på nytt`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        tilbakekreving.håndter(
            iverksettelse(),
            systemContext(behovObservatør = oppsamler),
        )

        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()), systemContext(behovObservatør = oppsamler))
        val vedtaksbrevBehov = oppsamler.behovListe.filterIsInstance<VedtaksbrevJournalføringBehov>()

        vedtaksbrevBehov.shouldHaveSize(2)
        vedtaksbrevBehov.map { it.brevId }.distinct().shouldHaveSize(1)
    }

    @Test
    fun `tilbakekreving skall være i DistribuerVedtak tilstand når JournalførVedtak er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                vedtakId = BigInteger("1234"),
                behandlingId = UUID.randomUUID(),
            ),
            systemContext(behovObservatør = oppsamler),
        )
        val brevId = (oppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId
        tilbakekreving.håndter(
            journalføring(brevId, tilbakekreving.eksternFagsak.eksternId),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe DistribuerVedtak
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
            lagreUttalelse(nåværendeBehandlingId(), UttalelseVurdering.JA, null, "", saksbehandlerContext(endringOppsamler))
            håndter(
                nåværendeBehandlingId(),
                saksbehandlerContext(endringOppsamler),
                faktastegVurdering(),
            )
            håndter(
                nåværendeBehandlingId(),
                saksbehandlerContext(endringOppsamler),
                periode = 1.januar(2021) til 31.januar(2021),
                foreldelseVurdering(),
            )
            håndter(
                nåværendeBehandlingId(),
                saksbehandlerContext(endringOppsamler),
                periode = 1.januar(2021) til 31.januar(2021),
                vurdering = forårsaketAvBruker().uaktsomt(),
            )
            håndterForeslåVedtak(nåværendeBehandlingId(), saksbehandlerContext(endringOppsamler))
            tilbakekreving.håndter(
                behandlingId = nåværendeBehandlingId(),
                sideeffektContext = beslutterContext(endringOppsamler),
                vurderinger = godkjenning(),
            )
        }
        return tilbakekreving
    }
}
