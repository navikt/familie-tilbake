package no.nav.tilbakekreving.statusmeldinger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.avslStatusmelding
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.e2e.kravgrunnlagMedBeløp
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtak
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.tilstand.Avsluttet
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.TilBehandling
import no.nav.tilbakekreving.vurderAlleSteg
import org.junit.jupiter.api.Test

class StatusmeldingAvslTest {
    @Test
    fun `avslStatusmelding avslutter behandlingen først når tilstanden er AvventerBrukerinfo`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context).apply {
            håndter(kravgrunnlag(), context)
        }
        tilbakekreving.håndter(
            avslStatusmelding(),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe AvventerFagsysteminfo

        tilbakekreving.apply {
            håndter(fagsysteminfoHendelse(), context)
        }
        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        tilbakekreving.apply {
            håndter(brukerinfoHendelse(), context)
        }
        tilbakekreving.tilstand shouldBe Avsluttet
    }

    @Test
    fun `avslStatusmelding avslutter behandlingen når den er i TilBehandling`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse).apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext()) {
                vurderFakta(faktastegVurdering())
            }
        }
        tilbakekreving.håndter(
            avslStatusmelding(),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe Avsluttet
    }

    @Test
    fun `avslStatusmelding sender behandlingen til TilBehandling og gjør behandlingen som UtenforScope når tilstand er i iverksetting`() {
        val periode = 1.januar(2025) til 31.januar(2025)
        val saksbehandlerContext = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext, periode)
        tilbakekreving.apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext) {
                foreslåVedtak()
            }
        }
        tilbakekreving.fatteVedtak()

        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.håndter(
                avslStatusmelding(),
                systemContext(),
            )
            tilbakekreving.tilstand shouldBe TilBehandling
        }
    }
}
