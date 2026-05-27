package no.nav.tilbakekreving.e2e.ytelser.arbeidsavklaringspenger
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.systemContext
import org.junit.jupiter.api.Test
import java.util.UUID

class ArbeidsavklaringspengerE2ETest {
    @Test
    fun `hopper over innhenting av fagsystem info`() {
        val observatør = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Arbeidsavklaringspenger,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(behovObservatør = observatør),
        )
        tilbakekreving.håndter(kravgrunnlag(), systemContext(behovObservatør = observatør))

        observatør.behovListe.size shouldBe 2
        observatør.behovListe.filterIsInstance<BrukerinfoBehov>().size shouldBe 1
        observatør.behovListe.filterIsInstance<FagsysteminfoBehov>().size shouldBe 1
    }
}
