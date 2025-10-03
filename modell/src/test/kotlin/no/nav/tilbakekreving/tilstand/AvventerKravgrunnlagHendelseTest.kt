package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class AvventerKravgrunnlagHendelseTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving i AvventerKravgrunnlag går videre med Kravgrunnlag`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler())

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag)

        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo
        oppsamler.behovListe.forOne {
            it shouldBeEqual FagsysteminfoBehov(
                eksternFagsakId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                eksternBehandlingId = kravgrunnlag.referanse,
                ytelse = Ytelse.Barnetrygd,
                vedtakGjelderId = kravgrunnlag.vedtakGjelder.ident,
            )
        }
    }
}
