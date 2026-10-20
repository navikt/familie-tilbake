package no.nav.tilbakekreving.tilstand
import io.kotest.inspectors.forOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.bruker
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.Kravstatuskode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
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
    fun `mottar nytt kravgrunnlag i avventer fagsysteminfo`() {
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse(),
            sideeffektContext = systemContext(),
        )

        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021))))
        tilbakekreving.håndter(kravgrunnlag, systemContext())
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.håndter(
            kravgrunnlag(
                vedtakId = kravgrunnlag.vedtakId,
                kravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                kravstatuskode = Kravstatuskode.ENDRET,
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021), ytelsesbeløp(tilbakekrevesBeløp = 1000.kroner)),
                    kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                ),
            ),
            systemContext(),
        )

        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext())

        tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())
            .faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now())
            .should {
                it.feilutbetaltePerioder[0].periode shouldBe (1.januar(2021) til 31.januar(2021))
                it.feilutbetaltePerioder[1].periode shouldBe (1.februar(2021) til 28.februar(2021))
            }
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
