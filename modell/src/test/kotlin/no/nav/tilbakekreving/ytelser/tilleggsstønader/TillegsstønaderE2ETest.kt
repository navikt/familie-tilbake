package no.nav.tilbakekreving.ytelser.tilleggsstønader

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.UtenforScopeException
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test

class TillegsstønaderE2ETest {
    @Test
    fun `hopper over innhenting av fagsystem info`() {
        val observatør = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Tillegsstønader,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(observatør, opprettTilbakekrevingHendelse)
        tilbakekreving.håndter(kravgrunnlag())

        observatør.behovListe.size shouldBe 1
        observatør.behovListe.single().shouldBeInstanceOf<BrukerinfoBehov>()
    }

    @Test
    fun `kan ikke håndtere kravgrunnlag som ikke gjelder person`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(ytelse = Ytelse.Tillegsstønader),
        )
        val tilbakekreving = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent)

        val exception = shouldThrow<UtenforScopeException> {
            tilbakekreving.håndter(kravgrunnlag(vedtakGjelder = KravgrunnlagHendelse.Aktør.Organisasjon("889640782")))
        }

        exception.utenforScope shouldBe UtenforScope.KravgrunnlagIkkePerson
    }
}
