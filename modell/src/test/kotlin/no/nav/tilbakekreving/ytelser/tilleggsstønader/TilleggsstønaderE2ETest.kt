package no.nav.tilbakekreving.ytelser.tilleggsstønader

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test

class TilleggsstønaderE2ETest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `hopper over innhenting av fagsystem info`() {
        val observatør = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(observatør, opprettTilbakekrevingHendelse, bigQueryService, EndringObservatørOppsamler())
        tilbakekreving.håndter(kravgrunnlag())

        observatør.behovListe.size shouldBe 1
        observatør.behovListe.single().shouldBeInstanceOf<BrukerinfoBehov>()
    }
}
