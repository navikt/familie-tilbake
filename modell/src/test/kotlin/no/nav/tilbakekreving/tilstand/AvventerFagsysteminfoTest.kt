package no.nav.tilbakekreving.tilstand
import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.systemContext
import org.junit.jupiter.api.Test
import java.util.UUID

class AvventerFagsysteminfoTest {
    @Test
    fun `tilbakekreving i AvventerFagsysteminfo går videre med fagsysteminfo`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(behovObservatør = oppsamler),
        )

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag, systemContext(behovObservatør = oppsamler))
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext())

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
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(),
        )

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag, systemContext())
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.håndter(fagsysteminfoHendelse(behandlendeEnhet = "0425"), systemContext())

        val enhet = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId()).hentBehandlingsinformasjon().enhet
        enhet?.kode shouldBe "0425"
        enhet?.navn shouldBe "Nav Solør"
    }

    @Test
    fun `skal ikke feile dersom vi prøver å hente URL for sak uten behandling`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingEvent,
            sideeffektContext = systemContext(),
        )

        val kravgrunnlag = kravgrunnlag()
        tilbakekreving.håndter(kravgrunnlag, systemContext())
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo
        tilbakekreving.hentTilbakekrevingUrl("https://tilbakekreving.ansatt.dev.nav.no") shouldBe "https://tilbakekreving.ansatt.dev.nav.no/fagsystem/BA/fagsak/101010"
    }
}
