package no.nav.tilbakekreving.tilstand

import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class AvventerFagsysteminfoTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `tilbakekreving i AvventerFagsysteminfo går videre med fagsysteminfo`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.håndter(fagsysteminfoHendelse())

        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        oppsamler.behovListe.forOne {
            it shouldBeEqual
                FagsysteminfoBehov(
                    eksternFagsakId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                    eksternBehandlingId = kravgrunnlag.referanse,
                    vedtakGjelderId = bruker().ident,
                    ytelse = opprettTilbakekrevingEvent.eksternFagsak.ytelse,
                )
        }
    }

    @Test
    fun `behandlende enhet blir oppdatert`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.håndter(fagsysteminfoHendelse(behandlendeEnhet = "0425"))

        val enhet = tilbakekreving.behandlingHistorikk.nåværende().entry.hentBehandlingsinformasjon().enhet
        enhet?.kode shouldBe "0425"
        enhet?.navn shouldBe "Nav Solør"
    }

    @Test
    fun `skal ikke feile dersom vi prøver å hente URL for sak uten behandling`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), oppsamler, opprettTilbakekrevingEvent, bigQueryService, EndringObservatørOppsamler(), features = defaultFeatures())

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag)
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo
        tilbakekreving.hentTilbakekrevingUrl("https://tilbakekreving.ansatt.dev.nav.no") shouldBe "https://tilbakekreving.ansatt.dev.nav.no/fagsystem/BA/fagsak/101010"
    }
}
