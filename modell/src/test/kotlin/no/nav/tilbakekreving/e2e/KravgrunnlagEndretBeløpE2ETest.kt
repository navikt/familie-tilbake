package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
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
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.lesContext
import no.nav.tilbakekreving.nåværendeBehandling
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.vilkårsvurderingsperiodeId
import no.nav.tilbakekreving.vurderAlleSteg
import org.junit.jupiter.api.Test

class KravgrunnlagEndretBeløpE2ETest {
    private val features = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to true))

    @Test
    fun `endret kravgrunnlag med høyere beløp etter påbegynt vurdering`() {
        val periode = 1.januar(2021) til 31.januar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext(features = features), periode)

        tilbakekreving.håndter(kravgrunnlagMedBeløp(periode to 3000.kroner), systemContext(features = features))

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
    fun `tilbakefører relevante steg når man tar i bruk kravgrunnlag med høyere beløp`() {
        val periode = 1.januar(2021) til 31.januar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext(features = features), periode)

        tilbakekreving.håndter(kravgrunnlagMedBeløp(periode to 3000.kroner), systemContext(features = features))

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
        val periode = 1.januar(2021) til 31.januar(2021)
        val saksbehandlerContext = saksbehandlerContext(features = features)

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext, periode)

        tilbakekreving.håndter(kravgrunnlagMedBeløp(periode to 1000.kroner), systemContext(features = features))

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
        val featuresUtenVisning = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to false))
        val periode = 1.januar(2021) til 31.januar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext(features = featuresUtenVisning), periode)

        tilbakekreving.håndter(kravgrunnlagMedBeløp(periode to 3000.kroner), systemContext(features = featuresUtenVisning))

        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekreving.validerInnenforScope(featuresUtenVisning)
        }
    }

    @Test
    fun `periode med endring i kravgrunnlaget får enda et nytt kravgrunnlag uten ny endring`() {
        val klokke = KlokkeStub(1.januar(2022))
        val saksbehandlerContext = saksbehandlerContext(features = features, klokke = klokke)
        val systemContext = systemContext(features = features, klokke = klokke)
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(
            kravgrunnlag = kravgrunnlagMedBeløp(
                periode1 to 2000.kroner,
                periode2 to 2000.kroner,
            ),
            context = systemContext,
        )

        val periode2Id = tilbakekreving.vilkårsvurderingsperiodeId(periode2)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering(perioder = listOf(periode1, periode2)))
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderVilkår(periode1, forårsaketAvBruker().uaktsomt())
            splittVilkårsvurdering(periode2Id)
            vurderVilkår(periode2, forårsaketAvBruker().grovtUaktsomt())
        }

        tilbakekreving.håndter(
            kravgrunnlagMedBeløp(
                periode1 to 3000.kroner,
                periode2 to 3000.kroner,
            ),
            systemContext,
        )
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            brukNyesteKravgrunnlag()
        }

        tilbakekreving.håndter(
            kravgrunnlagMedBeløp(
                periode1 to 4000.kroner,
                periode2 to 3000.kroner,
            ),
            systemContext,
        )
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            brukNyesteKravgrunnlag()
        }

        val expectedEndring = EndretPeriodeDto(
            fom = 1.januar(2021),
            tom = 31.januar(2021),
            gammelPeriode = PeriodeDto(
                fom = 1.januar(2021),
                tom = 31.januar(2021),
            ),
            nyttBeløp = 4000,
            gammeltBeløp = 3000,
        )

        tilbakekreving.tilFeilutbetalingFrontendDto(tilbakekreving.nåværendeBehandlingId(), klokke)
            .perioder
            .should { faktaPerioder ->
                faktaPerioder.single { it.fom == periode1.fom }.endringIKravgrunnlag shouldBe expectedEndring
                faktaPerioder.single { it.fom == periode2.fom }.endringIKravgrunnlag.shouldBeNull()
            }

        tilbakekreving.nåværendeBehandling()
            .vilkårsvurderingDto(lesContext(klokke = klokke))
            .vilkårsperioder
            .should { vilkårsperioder ->
                vilkårsperioder.single { it.vilkårsvurdering.fom == periode1.fom }
                    .vilkårsvurdering.endringIKravgrunnlag shouldBe expectedEndring
                vilkårsperioder.single { it.vilkårsvurdering.fom == periode2.fom }
                    .vilkårsvurdering.endringIKravgrunnlag.shouldBeNull()
            }
    }
}
