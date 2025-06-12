package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test

class AvventerFagsysteminfoTest {
    @Test
    fun `tilbakekreving i AvventerFagsysteminfo går videre med fagsysteminfo`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())

        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        oppsamler.behovListe.forOne {
            it shouldBeEqual
                FagsysteminfoBehov(
                    opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                    opprettTilbakekrevingEvent.eksternFagsak.ytelse,
                )
        }
    }
}
