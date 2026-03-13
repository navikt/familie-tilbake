package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksresultatDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalUnnlates
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtaksresultatE2ETest {
    @Test
    fun `hentVedtaksresultat returnerer beregningsresultat for full tilbakekreving`() {
        val behandler = Behandler.Saksbehandler("Z999999")
        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse(),
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
            features = defaultFeatures(),
        )

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.håndter(behandler, faktastegVurdering())
        tilbakekreving.trengerVarselbrev("Tekst fra saksbehandler")
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), null)

        tilbakekreving.håndter(behandler, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(behandler, 1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())

        val resultat = tilbakekreving.hentVedtaksresultatForFrontend()

        resultat.vedtaksresultat shouldBe VedtaksresultatDto.FullTilbakebetaling
        resultat.beregningsresultatsperioder shouldHaveSize 1

        val periode = resultat.beregningsresultatsperioder.first()
        periode.vurdering shouldBe BeregningsresultatVurderingDto.GrovUaktsomhet
        periode.feilutbetaltBeløp shouldBe 2000
        periode.andelAvBeløp shouldBe 100
        periode.tilbakekrevingsbeløp shouldBe 4000
    }

    @Test
    fun `hentVedtaksresultat returnerer ingen tilbakebetaling under 4x rettsgebyr`() {
        val behandler = Behandler.Saksbehandler("Z999999")
        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse(),
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
            features = defaultFeatures(),
        )

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.håndter(behandler, faktastegVurdering())
        tilbakekreving.trengerVarselbrev("Tekst fra saksbehandler")
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), null)

        tilbakekreving.håndter(behandler, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(
            behandler,
            1.januar(2021) til 31.januar(2021),
            forårsaketAvBruker().uaktsomt(unnlates = skalUnnlates()),
        )

        val resultat = tilbakekreving.hentVedtaksresultatForFrontend()

        resultat.vedtaksresultat shouldBe VedtaksresultatDto.IngenTilbakebetaling
        resultat.beregningsresultatsperioder.first().tilbakekrevingsbeløp shouldBe 0
    }
}
