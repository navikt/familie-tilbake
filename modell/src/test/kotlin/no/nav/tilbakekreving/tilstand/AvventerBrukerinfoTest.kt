package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forExactly
import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AvventerBrukerinfoTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `håndtering av AvventerBrukerinfo skal også oppdatere brevmottakerSteg i behandling`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.behandlingHistorikk.nåværende().entry.brevmottakerSteg?.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker>()
    }

    @Test
    fun `tilbakekreving med AvventerBrukerinfo går videre med brukerinfo`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.tilstand shouldBe TilBehandling
        oppsamler.behovListe.forOne {
            it shouldBeEqual BrukerinfoBehov(
                bruker().ident,
                tilbakekreving.eksternFagsak.ytelse,
            )
        }
    }

    @Test
    fun `tilbakekreving med AvventerBrukerinfo sender nytt behov for brukerinfo ved påminnelse`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()))

        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        oppsamler.behovListe.forExactly(2) {
            it shouldBeEqual BrukerinfoBehov(
                bruker().ident,
                tilbakekreving.eksternFagsak.ytelse,
            )
        }
    }
}
