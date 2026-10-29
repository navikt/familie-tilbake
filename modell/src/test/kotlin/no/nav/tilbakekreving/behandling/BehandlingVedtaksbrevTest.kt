package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.delerAvBeløp
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.skalUnnlates
import no.nav.tilbakekreving.vilkårsvurderingsperiodeId
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

class BehandlingVedtaksbrevTest {
    @ParameterizedTest
    @MethodSource("vurderinger")
    fun `Lager korrekte perioder for behandling`(
        forårsaketAvBruker: ForårsaketAvBruker,
        påkrevdeBegrunnelser: Set<VilkårsvurderingBegrunnelse>,
    ) {
        val vilkårsvurdering = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(
                utvidPerioder = listOf(
                    EksternFagsakRevurdering.UtvidetPeriode(
                        UUID.randomUUID(),
                        1.januar(2021) til 1.januar(2021),
                        1.januar(2021) til 31.januar(2021),
                    ),
                ),
            ),
            kravgrunnlag(
                perioder = listOf(kravgrunnlagPeriode(1.januar(2021) til 1.januar(2021))),
            ),
        )

        vilkårsvurdering.vurder(1.januar(2021) til 31.januar(2021), forårsaketAvBruker)

        val vurderingForPeriode = vilkårsvurdering.vurdertePerioderForBrev(emptySet()).single()
        vurderingForPeriode.periode shouldBe (1.januar(2021) til 31.januar(2021))
        vurderingForPeriode.meldingerTilSaksbehandler shouldBe emptySet()
        vurderingForPeriode.påkrevdeVurderinger shouldBe påkrevdeBegrunnelser
    }

    @Test
    fun `ulike vurderinger gir egne perioder i vedtaksbrevet`() {
        val januar = 1.januar(2021) til 31.januar(2021)
        val februar = 1.februar(2021) til 28.februar(2021)
        val mars = 1.mars(2021) til 31.mars(2021)
        val vilkårsvurdering = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(januar),
                    kravgrunnlagPeriode(februar),
                    kravgrunnlagPeriode(mars),
                ),
            ),
        )
        val januarId = vilkårsvurdering.vilkårsvurderingsperiodeId(januar)
        val februarId = vilkårsvurdering.vilkårsvurderingsperiodeId(februar)
        vilkårsvurdering.vurder(januarId, forårsaketAvBruker().uaktsomt(unnlates = skalIkkeUnnlates()))
        vilkårsvurdering.splittVilkårsvurdering(februarId)
        vilkårsvurdering.vurder(februarId, forårsaketAvNav().godTro())

        val perioder = vilkårsvurdering.vurdertePerioderForBrev(emptySet())

        perioder shouldBe listOf(
            BegrunnetPeriode(
                id = januarId,
                periode = januar,
                meldingerTilSaksbehandler = emptySet(),
                påkrevdeVurderinger = setOf(
                    VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                    VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR,
                    VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER,
                ),
            ),
            BegrunnetPeriode(
                id = februarId,
                periode = 1.februar(2021) til 31.mars(2021),
                meldingerTilSaksbehandler = emptySet(),
                påkrevdeVurderinger = setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_IKKE_I_BEHOLD),
            ),
        )
    }

    @Test
    fun `samlet vurdering slås sammen til en periode i vedtaksbrevet`() {
        val januar = 1.januar(2021) til 31.januar(2021)
        val vilkårsvurdering = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(januar),
                    kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                ),
            ),
        )
        val januarId = vilkårsvurdering.vilkårsvurderingsperiodeId(januar)
        vilkårsvurdering.vurder(januarId, forårsaketAvBruker().uaktsomt(unnlates = skalIkkeUnnlates()))

        val periode = vilkårsvurdering.vurdertePerioderForBrev(emptySet()).single()

        periode.id shouldBe januarId
        periode.periode shouldBe (1.januar(2021) til 28.februar(2021))
    }

    companion object {
        @JvmStatic
        fun vurderinger(): List<Arguments> {
            return listOf(
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    forårsaketAvNav().godTro(beløpIBehold = delerAvBeløp(1400.kroner)),
                    setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_I_BEHOLD),
                ),
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    forårsaketAvNav().godTro(),
                    setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_IKKE_I_BEHOLD),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, uaktsomt, skal ikke unnlates",
                    forårsaketAvBruker().uaktsomt(unnlates = skalIkkeUnnlates()),
                    setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, uaktsomt, unnlates",
                    forårsaketAvBruker().uaktsomt(unnlates = skalUnnlates()),
                    setOf(VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, grovt uaktsomt",
                    forårsaketAvBruker().grovtUaktsomt(),
                    setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, forsettelig",
                    forårsaketAvBruker().medForsett(),
                    setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forsårsaket av Nav, burde forstått",
                    forårsaketAvNav().burdeForstått(),
                    setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forsårsaket av Nav, forstod",
                    forårsaketAvNav().forstod(),
                    setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
            )
        }
    }
}
