package no.nav.tilbakekreving.tilstand

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class SendVarselbrevTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving ved AutomatiskVarselbrev er i SendVarselbrev tilstand når bruker er håndtert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures(Toggle.SendAutomatiskVarselbrev to true))
        val bruker = brukerinfoHendelse()
        val kravgrunnlag = kravgrunnlag()
        val fagsak = fagsysteminfoHendelse()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.håndter(fagsak)
        tilbakekreving.håndter(bruker)

        tilbakekreving.tilstand shouldBe SendVarselbrev
    }
}
