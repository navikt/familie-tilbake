package no.nav.tilbakekreving.tilstand

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.JournalføringBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.iverksettelse
import no.nav.tilbakekreving.journalføring
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class JournalførVedtakTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving skall være i journalføring tilstand når iverksettelse er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        tilbakekreving.håndter(
            iverksettelse(),
        )
        tilbakekreving.tilstand shouldBe JournalførVedtak
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
        )
        val brevId = (oppsamler.behovListe.last() as JournalføringBehov).brevId
        tilbakekreving.håndter(
            journalføring(brevId),
        )
        tilbakekreving.tilstand shouldBe DistribuerVedtak
    }

    private fun tilbakekrevingKlarTilJournalføring(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        oppsamler: BehovObservatørOppsamler,
        endringOppsamler: EndringObservatørOppsamler = EndringObservatørOppsamler(),
    ): Tilbakekreving {
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringOppsamler, features = defaultFeatures())
        tilbakekreving.apply {
            håndter(kravgrunnlag())
            håndter(fagsysteminfoHendelse())
            håndter(brukerinfoHendelse())
            behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")
            håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                faktastegVurdering(),
            )
            håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                periode = 1.januar(2021) til 31.januar(2021),
                foreldelseVurdering(),
            )
            håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                periode = 1.januar(2021) til 31.januar(2021),
                vurdering = forårsaketAvBruker().uaktsomt(),
            )
            håndterForeslåVedtak(Behandler.Saksbehandler("Ansvarlig saksbehandler"))
            tilbakekreving.håndter(
                beslutter = Behandler.Saksbehandler("Z999999"),
                vurderinger = godkjenning(),
            )
        }
        return tilbakekreving
    }
}
