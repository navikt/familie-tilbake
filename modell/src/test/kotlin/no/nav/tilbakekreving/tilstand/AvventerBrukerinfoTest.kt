package no.nav.tilbakekreving.tilstand
import io.kotest.inspectors.forExactly
import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.systemContext
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AvventerBrukerinfoTest {
    @Test
    fun `håndtering av AvventerBrukerinfo skal også oppdatere brevmottakerSteg i behandling`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(),
        )

        tilbakekreving.håndter(kravgrunnlag(), systemContext())
        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext())
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext())
    }

    @Test
    fun `tilbakekreving med AvventerBrukerinfo går videre med brukerinfo`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(behovObservatør = oppsamler),
        )

        tilbakekreving.håndter(kravgrunnlag(), systemContext())
        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext(behovObservatør = oppsamler))
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext())

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
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(behovObservatør = oppsamler),
        )

        tilbakekreving.håndter(kravgrunnlag(), systemContext())
        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext(behovObservatør = oppsamler))
        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()), systemContext(behovObservatør = oppsamler))

        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        oppsamler.behovListe.forExactly(2) {
            it shouldBeEqual BrukerinfoBehov(
                bruker().ident,
                tilbakekreving.eksternFagsak.ytelse,
            )
        }
    }
}
