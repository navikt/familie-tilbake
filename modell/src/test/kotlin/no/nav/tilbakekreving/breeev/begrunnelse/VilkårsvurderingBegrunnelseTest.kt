package no.nav.tilbakekreving.breeev.begrunnelse

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.prosentReduksjon
import no.nav.tilbakekreving.test.skalUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class VilkårsvurderingBegrunnelseTest {
    @ParameterizedTest
    @MethodSource("begrunnelserForVilkårsvurdering")
    fun `velger riktige påkrevde begrunnelser for vilkårsvurdering`(
        vurdering: ForårsaketAvBruker,
        forventedeBegrunnelser: Set<VilkårsvurderingBegrunnelse>,
    ) {
        val vilkårsvurdering = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021))),
            ),
        )

        vilkårsvurdering.vurder(1.januar(2021) til 31.januar(2021), vurdering)

        val vurderinger = vilkårsvurdering.vurdertePerioderForBrev(emptySet())
        vurderinger shouldHaveSize 1
        vurderinger.single().påkrevdeVurderinger shouldBe forventedeBegrunnelser
    }

    companion object {
        private fun begrunnelse(
            name: String,
            vurdering: ForårsaketAvBruker,
            forventedeBegrunnelser: Set<VilkårsvurderingBegrunnelse>,
        ): Arguments {
            return Arguments.argumentSet(name, vurdering, forventedeBegrunnelser)
        }

        @JvmStatic
        fun begrunnelserForVilkårsvurdering(): List<Arguments> {
            return listOf(
                begrunnelse(
                    name = "forårsaket av nav, uaktsomt, unnlates ikke, ingen reduksjon",
                    vurdering = forårsaketAvBruker().uaktsomt(),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR,
                        VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av bruker, uaktsomt, unnlates ikke, redusert",
                    vurdering = forårsaketAvBruker().uaktsomt(reduksjon = 50.prosentReduksjon),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR,
                        VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av bruker, uaktsomt, unnlates",
                    vurdering = forårsaketAvBruker().uaktsomt(unnlates = skalUnnlates()),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.INGEN_TILBAKEKREVING,
                        VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av bruker, grovt uaktsomt, ingen reduksjon",
                    vurdering = forårsaketAvBruker().grovtUaktsomt(reduksjon = ingenReduksjon()),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av bruker, grovt uaktsomt, redusert",
                    vurdering = forårsaketAvBruker().grovtUaktsomt(reduksjon = 50.prosentReduksjon),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av bruker, forsettelig",
                    vurdering = forårsaketAvBruker().medForsett(),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av Nav, god tro, beløp i behold",
                    vurdering = forårsaketAvNav().godTro(beløpIBehold = 1500.kroner),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_I_BEHOLD,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av Nav, god tro, beløp ikke i behold",
                    vurdering = forårsaketAvNav().godTro(beløpIBehold = null),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_IKKE_I_BEHOLD,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av Nav, burde forstått, ingen reduksjon",
                    vurdering = forårsaketAvNav().burdeForstått(
                        uaktsomt(
                            reduksjon = ingenReduksjon(),
                        ),
                    ),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR,
                        VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av Nav, burde forstått, redusert",
                    vurdering = forårsaketAvNav().burdeForstått(
                        uaktsomt(
                            reduksjon = 50.prosentReduksjon,
                        ),
                    ),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.TILBAKEKREVES,
                        VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR,
                        VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER,
                    ),
                ),
                begrunnelse(
                    name = "forårsaket av Nav, burde forstått, unnlates",
                    vurdering = forårsaketAvNav().burdeForstått(
                        uaktsomt(
                            unnlates = skalUnnlates(),
                        ),
                    ),
                    forventedeBegrunnelser = setOf(
                        VilkårsvurderingBegrunnelse.INGEN_TILBAKEKREVING,
                        VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR,
                    ),
                ),
            )
        }
    }
}
