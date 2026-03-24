package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
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

        val vurderingForPeriode = vilkårsvurdering.vurdertePerioderForBrev(emptySet()).single()
        vurderingForPeriode.periode shouldBe (1.januar(2021) til 31.januar(2021))
        vurderingForPeriode.meldingerTilSaksbehandler shouldBe emptySet()
        vurderingForPeriode.påkrevdeVurderinger shouldBe påkrevdeBegrunnelser
    }

    companion object {
        @JvmStatic
        fun vurderinger(): List<Arguments> {
            return listOf(
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    forårsaketAvNav().godTro(beløpIBehold = 1400.kroner),
                    setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_I_BEHOLD),
                ),
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    forårsaketAvNav().godTro(beløpIBehold = null),
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
                    setOf(VilkårsvurderingBegrunnelse.INGEN_TILBAKEKREVING, VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR),
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
                    setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES),
                ),
            )
        }
    }
}
