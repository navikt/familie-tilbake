package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test

class AvventerKravgrunnlagHendelseTest {
    @Test
    fun `tilbakekreving i AvventerKravgrunnlag går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(kravgrunnlag())

        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo
        oppsamler.behovListe.forOne {
            it shouldBeEqual FagsysteminfoBehov(
                eksternFagsakId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                ytelse = Ytelse.Barnetrygd,
            )
        }
    }
}
