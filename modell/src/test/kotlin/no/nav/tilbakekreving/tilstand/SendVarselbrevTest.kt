package no.nav.tilbakekreving.tilstand

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingEvent
import no.nav.tilbakekreving.varselbrev
import org.junit.jupiter.api.Test

class SendVarselbrevTest {
    @Test
    fun `tilbakekreving i SendVarselbrev går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(VarselbrevSendtHendelse(varselbrev()))

        tilbakekreving.tilstand shouldBe TilBehandling
    }
}
