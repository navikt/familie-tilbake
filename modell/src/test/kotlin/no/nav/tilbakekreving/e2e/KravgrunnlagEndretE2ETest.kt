package no.nav.tilbakekreving.e2e
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Test

class KravgrunnlagEndretE2ETest {
    @Test
    fun `endret kravgrunnlag med høyere beløp etter påbegynt vurdering`() {
        val features = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to true))
        val periode = 1.januar(2021) til 31.januar(2021)
        val opprinneligKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = periode)))

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = opprinneligKravgrunnlag)

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(features = features)) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }

        val oppdatertKravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(
                    periode = periode,
                    ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = 3000.kroner),
                ),
            ),
        )
        tilbakekreving.håndter(oppdatertKravgrunnlag, systemContext(features = features))

        val behandlingDto = tilbakekreving.frontendDtoForBehandling(
            tilbakekreving.nåværendeBehandlingId(),
            saksbehandlerContext(features = features),
            true,
            BehandlerRolle.SAKSBEHANDLER,
        )

        behandlingDto.endretKravgrunnlag.shouldNotBeNull {
            gammeltBeløp shouldBe 2000
            nyttBeløp shouldBe 3000
            gammelPeriode shouldBe periode
            nyPeriode shouldBe periode
        }

        shouldNotThrowAny {
            tilbakekreving.validerInnenforScope(features)
        }

        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
                vurderVilkår(periode, forårsaketAvBruker().grovtUaktsomt())
            }
        }
    }

    @Test
    fun `endret kravgrunnlag når toggle er avslått etter påbegynt vurdering`() {
        val features = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to false))
        val periode = 1.januar(2021) til 31.januar(2021)
        val opprinneligKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = periode)))

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = opprinneligKravgrunnlag)

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(features = features)) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }

        val oppdatertKravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(
                    periode = periode,
                    ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = 3000.kroner),
                ),
            ),
        )
        tilbakekreving.håndter(oppdatertKravgrunnlag, systemContext(features = features))

        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(features)
        }
    }
}
