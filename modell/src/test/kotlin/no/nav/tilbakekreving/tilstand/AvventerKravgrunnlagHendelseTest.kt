package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingEvent
import org.junit.jupiter.api.Test

class AvventerKravgrunnlagHendelseTest {
    @Test
    fun `tilbakekreving i AvventerKravgrunnlag går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(kravgrunnlag())

        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo
        oppsamler.fagsysteminfoBehov.size shouldBe 1
        oppsamler.fagsysteminfoBehov.forOne {
            it shouldBeEqual
                FagsysteminfoBehov(
                    opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                    Fagsystem.BA,
                    Ytelsestype.BARNETRYGD,
                )
        }
    }
}
