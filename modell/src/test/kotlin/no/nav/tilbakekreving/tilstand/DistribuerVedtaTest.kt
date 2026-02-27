package no.nav.tilbakekreving.tilstand

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.JournalføringBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.distribusjon
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.forårsaketAvBrukerUaktsomt
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.iverksettelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.journalføring
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.util.UUID

class DistribuerVedtaTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving skall være i DistribuerVedtak tilstand når JournalførVedtak er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        val brevId = (oppsamler.behovListe.last() as JournalføringBehov).brevId
        tilbakekreving.håndter(
            JournalføringHendelse(
                brevId = brevId,
                journalpostId = "123",
            ),
        )
        tilbakekreving.tilstand shouldBe DistribuerVedtak
    }

    @Test
    fun `tilbakekreving skall være i Avsluttet tilstand når DistribuerVedtak er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingKlarTilJournalføring(opprettTilbakekrevingEvent, oppsamler)
        val brevId = (oppsamler.behovListe.last() as JournalføringBehov).brevId
        tilbakekreving.håndter(
            journalføring(brevId),
        )
        tilbakekreving.håndter(
            distribusjon(),
        )
        tilbakekreving.tilstand shouldBe Avsluttet
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
                periode = 1.januar til 31.januar,
                foreldelseVurdering(),
            )
            håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                periode = 1.januar til 31.januar,
                vurdering = forårsaketAvBrukerUaktsomt(),
            )
            håndterForeslåVedtak(Behandler.Saksbehandler("Ansvarlig saksbehandler"))
            tilbakekreving.håndter(
                beslutter = Behandler.Saksbehandler("Z999999"),
                vurderinger = godkjenning(),
            )
            tilbakekreving.håndter(
                iverksettelse(),
            )
        }
        return tilbakekreving
    }
}
