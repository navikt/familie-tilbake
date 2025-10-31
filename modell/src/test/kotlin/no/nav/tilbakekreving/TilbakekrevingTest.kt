package no.nav.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import org.junit.jupiter.api.Test
import java.util.UUID

class TilbakekrevingTest {
    private val bigQueryService = BigQueryServiceStub()
    private val endringObservatør = EndringObservatørOppsamler()

    @Test
    fun `oppretter tilbakekreving`() {
        val opprettEvent = opprettTilbakekrevingHendelse(opprettelsesvalg = Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), BehovObservatørOppsamler(), opprettEvent, bigQueryService, endringObservatør, varselbrevEnabled = true)

        tilbakekreving.tilstand shouldBe AvventerKravgrunnlag
    }
}
