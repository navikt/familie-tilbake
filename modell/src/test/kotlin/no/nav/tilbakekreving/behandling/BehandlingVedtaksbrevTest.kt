package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.PåkrevdBegrunnelse
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.forårsaketAvBrukerGrovtUaktsomt
import no.nav.tilbakekreving.forårsaketAvBrukerMedForsett
import no.nav.tilbakekreving.forårsaketAvBrukerUaktsomt
import no.nav.tilbakekreving.forårsaketAvNavBurdeForstått
import no.nav.tilbakekreving.forårsaketAvNavForstod
import no.nav.tilbakekreving.godTro
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.skalIkkeUnnlates
import no.nav.tilbakekreving.unnlates
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

class BehandlingVedtaksbrevTest {
    @ParameterizedTest
    @MethodSource("vurderinger")
    fun `Lager korrekte perioder for behandling`(
        forårsaketAvBruker: ForårsaketAvBruker,
        påkrevdeBegrunnelser: Set<PåkrevdBegrunnelse>,
    ) {
        val vilkårsvurdering = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(
                utvidPerioder = listOf(
                    EksternFagsakRevurdering.UtvidetPeriode(
                        UUID.randomUUID(),
                        1.januar til 1.januar,
                        1.januar til 31.januar,
                    ),
                ),
            ),
            kravgrunnlag(
                perioder = listOf(kravgrunnlagPeriode(1.januar til 1.januar)),
            ),
        )

        vilkårsvurdering.vurder(1.januar til 31.januar, forårsaketAvBruker)

        vilkårsvurdering.vurdertePerioderForBrev() shouldBe listOf(
            BegrunnetPeriode(1.januar til 31.januar, påkrevdeBegrunnelser),
        )
    }

    companion object {
        @JvmStatic
        fun vurderinger(): List<Arguments> {
            return listOf(
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    godTro(NivåAvForståelse.GodTro.BeløpIBehold.Ja(1400.kroner)),
                    emptySet<PåkrevdBegrunnelse>(),
                ),
                Arguments.argumentSet(
                    "God tro, beløp i behold",
                    godTro(NivåAvForståelse.GodTro.BeløpIBehold.Nei),
                    emptySet<PåkrevdBegrunnelse>(),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, uaktsomt, skal ikke unnlates",
                    forårsaketAvBrukerUaktsomt(skalIkkeUnnlates()),
                    setOf(PåkrevdBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR, PåkrevdBegrunnelse.SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, uaktsomt, unnlates",
                    forårsaketAvBrukerUaktsomt(unnlates()),
                    setOf(PåkrevdBegrunnelse.UNNLATES_4_RETTSGEBYR),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, grovt uaktsomt",
                    forårsaketAvBrukerGrovtUaktsomt(),
                    setOf(PåkrevdBegrunnelse.SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forårsaket av bruker, forsettelig",
                    forårsaketAvBrukerMedForsett(),
                    setOf(PåkrevdBegrunnelse.SÆRLIGE_GRUNNER),
                ),
                Arguments.argumentSet(
                    "Forsårsaket av Nav, burde forstått",
                    forårsaketAvNavBurdeForstått(),
                    emptySet<PåkrevdBegrunnelse>(),
                ),
                Arguments.argumentSet(
                    "Forsårsaket av Nav, burde forstått",
                    forårsaketAvNavForstod(),
                    emptySet<PåkrevdBegrunnelse>(),
                ),
            )
        }
    }
}
