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
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.skalUnnlates
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

        vilkårsvurdering.vurdertePerioderForBrev() shouldBe listOf(
            BegrunnetPeriode(1.januar(2021) til 31.januar(2021), påkrevdeBegrunnelser),
        )
    }

    companion object {
        @JvmStatic
        fun vurderinger(): List<Arguments> {
            return listOf(
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    forårsaketAvNav().godTro(beløpIBehold = 1400.kroner),
                    emptySet<VilkårsvurderingBegrunnelse>(),
                ),
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    forårsaketAvNav().godTro(beløpIBehold = null),
                    emptySet<VilkårsvurderingBegrunnelse>(),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, uaktsomt, skal ikke unnlates",
                    forårsaketAvBruker().uaktsomt {
                        skalUnnlates = skalIkkeUnnlates()
                    },
                    setOf(VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, uaktsomt, unnlates",
                    forårsaketAvBruker().uaktsomt {
                        skalUnnlates = skalUnnlates()
                    },
                    setOf(VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, grovt uaktsomt",
                    forårsaketAvBruker().grovtUaktsomt(),
                    setOf(VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, forsettelig",
                    forårsaketAvBruker().medForsett(),
                    setOf(VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forsårsaket av Nav, burde forstått",
                    forårsaketAvNav().burdeForstått(),
                    emptySet<VilkårsvurderingBegrunnelse>(),
                ),
                Arguments.argumentSet(
                    "Forsårsaket av Nav, forstod",
                    forårsaketAvNav().forstod(),
                    emptySet<VilkårsvurderingBegrunnelse>(),
                ),
            )
        }
    }
}
