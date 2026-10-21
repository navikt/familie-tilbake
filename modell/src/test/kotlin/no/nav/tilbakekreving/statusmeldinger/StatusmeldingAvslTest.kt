package no.nav.tilbakekreving.statusmeldinger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.avslStatusmelding
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
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
import no.nav.tilbakekreving.vurderAlleSteg
import org.junit.jupiter.api.Test

class StatusmeldingAvslTest {
    @Test
    fun `avslStatusmelding feiler når tilstanden er før til_behandling`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context).apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                avslStatusmelding(),
                systemContext(),
            )
        }
    }

    @Test
    fun `avslStatusmelding feiler etter behandling er iverksatt`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context).apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                avslStatusmelding(),
                systemContext(),
            )
        }
    }

    @Test
    fun `avslStatusmelding avslutter behandling når den er i til_behandling`() {
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
    fun `avslStatusmelding avslutter behandling når den er i IverksettVedtak tilstand`() {
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

        tilbakekreving.håndter(
            avslStatusmelding(),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe Avsluttet
    }
}
