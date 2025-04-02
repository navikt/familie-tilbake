package no.nav.tilbakekreving.tilstand

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingEvent
import org.junit.jupiter.api.Test
import java.util.UUID

class SendVarselbrevTest {
    @Test
    fun `tilbakekreving i SendVarselbrev går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(
            FagsysteminfoHendelse(
                eksternId = UUID.randomUUID().toString(),
            ),
        )
        tilbakekreving.håndter(VarselbrevSendtHendelse)

        tilbakekreving.tilstand shouldBe TilBehandling
    }
}
