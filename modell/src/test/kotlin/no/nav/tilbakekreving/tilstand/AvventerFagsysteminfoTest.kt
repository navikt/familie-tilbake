package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingEvent
import org.junit.jupiter.api.Test

class AvventerFagsysteminfoTest {
    @Test
    fun `tilbakekreving i AvventerKravgrunnlag går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())

        tilbakekreving.tilstand shouldBe SendVarselbrev
        oppsamler.varselbrevBehov.size shouldBe 1
        oppsamler.varselbrevBehov.forOne {
            it shouldBe VarselbrevBehov("wip")
        }
    }
}
