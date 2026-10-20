package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldContain
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
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.ArsakTilTilbakeforingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FjernetPeriodeDto
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
import org.junit.jupiter.api.Test

class KravgrunnlagEndretPeriodeE2ETest {
    private val features = defaultFeatures(featureOverrides = arrayOf(Toggle.EndretKravgrunnlagVisning to true))

    @Test
    fun `kravgrunnlag med ny periode tilbakefører forhåndsvarsel og legger til periode i alle steg`() {
        val saksbehandlerContext = saksbehandlerContext(features = features, klokke = KlokkeStub(1.januar(2022)))
        val systemContext = systemContext(features = features, klokke = KlokkeStub(1.januar(2022)))
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(
            kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner),
            context = systemContext,
        )

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }

        val oppdatertKravgrunnlag = kravgrunnlagMedBeløp(
            periode to 2000.kroner,
            nyPeriode to 2000.kroner,
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

        tilbakekreving.nåværendeBehandling().vilkårsvurderingDto(lesContext(klokke = KlokkeStub(1.januar(2022)))).vilkårsperioder.should {
            it shouldHaveSize 2
            it.single { vilkårsperiode -> vilkårsperiode.vilkårsvurdering.fom == periode.fom }.vilkårsvurdering.tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
            it.single { vilkårsperiode -> vilkårsperiode.vilkårsvurdering.fom == nyPeriode.fom }.vilkårsvurdering.tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
        }
    }

    @Test
    fun `kravgrunnlag med fjernet periode`() {
        val saksbehandlerContext = saksbehandlerContext(features = features, klokke = KlokkeStub(1.januar(2022)))
        val systemContext = systemContext(features = features, klokke = KlokkeStub(1.januar(2022)))
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)

        val tilbakekreving = tilbakekrevingTilBehandling(
            kravgrunnlag = kravgrunnlagMedBeløp(
                periode1 to 2000.kroner,
                periode2 to 2000.kroner,
            ),
            context = systemContext,
        )

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering(perioder = listOf(periode1, periode2)))
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderVilkår(periode1, forårsaketAvBruker().uaktsomt())
        }

        val oppdatertKravgrunnlag = kravgrunnlagMedBeløp(periode1 to 2000.kroner)
        tilbakekreving.håndter(oppdatertKravgrunnlag, systemContext)

        tilbakekreving.frontendDtoForBehandling(
            tilbakekreving.nåværendeBehandlingId(),
            saksbehandlerContext,
            true,
            BehandlerRolle.SAKSBEHANDLER,
        ).endretKravgrunnlag.shouldNotBeNull {
            this.gammeltBeløp shouldBe 4000
            this.nyttBeløp shouldBe 2000
            this.gammelPeriode shouldBe (1.januar(2021) til 28.februar(2021))
            this.nyPeriode shouldBe periode1
            this.endringer shouldContain FjernetPeriodeDto(periode2.fom, periode2.tom, 2000)
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            brukNyesteKravgrunnlag()
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FAKTA skalHaStatus Behandlingsstegstatus.TILBAKEFØRT

        tilbakekreving.faktastegFrontendDto(tilbakekreving.nåværendeBehandlingId()).should {
            it.feilutbetaltePerioder shouldHaveSize 1
            it.feilutbetaltePerioder[0].periode shouldBe periode1
        }

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext) {
            vurderFakta(faktastegVurdering(perioder = listOf(periode1)))
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.FORHÅNDSVARSEL skalHaStatus Behandlingsstegstatus.UTFØRT

        tilbakekreving.nåværendeBehandling().foreldelsestegDto.tilFrontendDto(saksbehandlerContext).should {
            it.foreldetPerioder shouldHaveSize 1
            it.foreldetPerioder[0].periode shouldBe periode1
            it.foreldetPerioder[0].foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET
        }

        tilbakekreving.nåværendeBehandling() skalHaSteg Behandlingssteg.VILKÅRSVURDERING skalHaStatus Behandlingsstegstatus.TILBAKEFØRT

        tilbakekreving.nåværendeBehandling().vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext).should {
            it.perioder shouldHaveSize 1
            it.perioder[0].periode shouldBe periode1
        }
    }
}
