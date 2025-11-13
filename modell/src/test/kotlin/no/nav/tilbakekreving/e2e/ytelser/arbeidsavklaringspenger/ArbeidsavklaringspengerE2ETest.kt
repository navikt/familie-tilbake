package no.nav.tilbakekreving.e2e.ytelser.arbeidsavklaringspenger

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class ArbeidsavklaringspengerE2ETest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `hopper over innhenting av fagsystem info`() {
        val observatør = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Arbeidsavklaringspenger,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), observatør, opprettTilbakekrevingHendelse, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag())

        observatør.behovListe.size shouldBe 2
        observatør.behovListe.filterIsInstance<BrukerinfoBehov>().size shouldBe 1
        observatør.behovListe.filterIsInstance<FagsysteminfoBehov>().size shouldBe 1
    }
}
