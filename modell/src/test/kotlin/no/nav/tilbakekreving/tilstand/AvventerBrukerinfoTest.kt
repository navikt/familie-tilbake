package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingEvent
import org.junit.jupiter.api.Test

class AvventerBrukerinfoTest {
    @Test
    fun `håndtering av AvventerBrukerinfo skal også oppdatere brevmottakerSteg i behandling`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.behandlingHistorikk.nåværende().entry.brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER
    }

    @Test
    fun `tilbakekreving med AvventerBrukerinfo går videre med brukerinfo`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.tilstand shouldBe SendVarselbrev
        oppsamler.behovListe.forOne {
            it shouldBeEqual
                BrukerinfoBehov(
                    bruker().ident,
                    tilbakekreving.eksternFagsak.fagsystem,
                )
        }
    }
}
