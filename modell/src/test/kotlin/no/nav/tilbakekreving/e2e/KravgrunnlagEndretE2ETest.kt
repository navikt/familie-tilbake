package no.nav.tilbakekreving.e2e
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.assertions.skalHaStatus
import no.nav.tilbakekreving.assertions.skalHaSteg
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandling
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.februar
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
    fun `kravgrunnlag med ny periode tilbakefører forhåndsvarsel og legger til periode i alle steg`() {
        val features = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to true))
        val saksbehandlerContext = saksbehandlerContext(features = features, klokke = KlokkeStub(1.januar(2022)))
        val systemContext = systemContext(features = features, klokke = KlokkeStub(1.januar(2022)))
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(
            kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = periode))),
            context = systemContext,
        )

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }

        val oppdatertKravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(periode = periode),
                kravgrunnlagPeriode(periode = nyPeriode),
            ),
        )
        tilbakekreving.håndter(oppdatertKravgrunnlag, systemContext)

        tilbakekreving.frontendDtoForBehandling(
            tilbakekreving.nåværendeBehandlingId(),
            saksbehandlerContext,
            true,
            BehandlerRolle.SAKSBEHANDLER,
        ).endretKravgrunnlag.shouldNotBeNull {
            this.gammeltBeløp shouldBe 2000
            this.nyttBeløp shouldBe 4000
            this.gammelPeriode shouldBe periode
            this.nyPeriode shouldBe (1.januar(2021) til 28.februar(2021))
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            brukNyesteKravgrunnlag()
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FAKTA skalHaStatus Behandlingsstegstatus.TILBAKEFØRT

        tilbakekreving.faktastegFrontendDto(tilbakekreving.nåværendeBehandlingId()).should {
            it.feilutbetaltePerioder shouldHaveSize 2
            it.feilutbetaltePerioder[1].periode shouldBe nyPeriode
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering(perioder = listOf(periode, nyPeriode)))
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORHÅNDSVARSEL skalHaStatus Behandlingsstegstatus.TILBAKEFØRT

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORELDELSE skalHaStatus Behandlingsstegstatus.UTFØRT

        tilbakekreving.nåværendeBehandling().foreldelsestegDto.tilFrontendDto(saksbehandlerContext).should {
            it.foreldetPerioder shouldHaveSize 2
            it.foreldetPerioder[1].periode shouldBe nyPeriode
            it.foreldetPerioder[1].foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.VILKÅRSVURDERING skalHaStatus Behandlingsstegstatus.TILBAKEFØRT

        tilbakekreving.nåværendeBehandling().vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext).should {
            it.perioder shouldHaveSize 2
            it.perioder[1].periode shouldBe nyPeriode
        }
    }

    @Test
    fun `tilbakefører relevante steg når man tar i bruk kravgrunnlag med høyere beløp`() {
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

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(features = features)) {
            brukNyesteKravgrunnlag()
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FAKTA skalHaStatus Behandlingsstegstatus.TILBAKEFØRT

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(features = features)) {
            vurderFakta(faktastegVurdering())
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORHÅNDSVARSEL skalHaStatus Behandlingsstegstatus.TILBAKEFØRT
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORELDELSE skalHaStatus Behandlingsstegstatus.TILBAKEFØRT
            vurderForeldelse(periode, foreldelseVurdering())
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.VILKÅRSVURDERING skalHaStatus Behandlingsstegstatus.TILBAKEFØRT
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FAKTA skalHaStatus Behandlingsstegstatus.UTFØRT
        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORHÅNDSVARSEL skalHaStatus Behandlingsstegstatus.UTFØRT
        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.VILKÅRSVURDERING skalHaStatus Behandlingsstegstatus.UTFØRT
    }

    @Test
    fun `tilbakefører relevante steg når man tar i bruk kravgrunnlag med lavere beløp`() {
        val features = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to true))
        val periode = 1.januar(2021) til 31.januar(2021)
        val opprinneligKravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode = periode)))
        val saksbehandlerContext = saksbehandlerContext(features = features)

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = opprinneligKravgrunnlag)

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }

        val oppdatertKravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(
                    periode = periode,
                    ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = 1000.kroner),
                ),
            ),
        )
        tilbakekreving.håndter(oppdatertKravgrunnlag, systemContext(features = features))

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            brukNyesteKravgrunnlag()
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FAKTA skalHaStatus Behandlingsstegstatus.TILBAKEFØRT
        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORHÅNDSVARSEL skalHaStatus Behandlingsstegstatus.VENTER

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering())
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FAKTA skalHaStatus Behandlingsstegstatus.UTFØRT
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORHÅNDSVARSEL skalHaStatus Behandlingsstegstatus.UTFØRT
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORELDELSE skalHaStatus Behandlingsstegstatus.TILBAKEFØRT
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderForeldelse(periode, foreldelseVurdering())
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORELDELSE skalHaStatus Behandlingsstegstatus.UTFØRT
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.VILKÅRSVURDERING skalHaStatus Behandlingsstegstatus.TILBAKEFØRT
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
            tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.VILKÅRSVURDERING skalHaStatus Behandlingsstegstatus.UTFØRT
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
