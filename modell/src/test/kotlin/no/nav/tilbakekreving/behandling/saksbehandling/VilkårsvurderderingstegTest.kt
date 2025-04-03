package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.HistorikkStub
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.til
import org.junit.jupiter.api.Test

class VilkårsvurderderingstegTest {
    @Test
    fun `vilkårsvurdering på en av to perioder`() {
        var vilkårsvurderderingsteg =
            Vilkårsvurderderingsteg.opprett(
                HistorikkStub.fakeReferanse(
                    kravgrunnlag(
                        perioder =
                            listOf(
                                kravgrunnlagPeriode(1.januar til 31.januar),
                                kravgrunnlagPeriode(1.februar til 28.februar),
                            ),
                    ),
                ),
            )
        vilkårsvurderderingsteg.vurder(
            1.januar til 31.januar,
            Vilkårsvurderderingsteg.Vurdering.GodTro(
                beløpIBehold = Vilkårsvurderderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                "Brukeren brukte alt på en tur til Vegas",
            ),
        )

        vilkårsvurderderingsteg.erFullstending() shouldBe false
    }

    @Test
    fun `vilkårsvurdering på begge perioder`() {
        var vilkårsvurderderingsteg =
            Vilkårsvurderderingsteg.opprett(
                HistorikkStub.fakeReferanse(
                    kravgrunnlag(
                        perioder =
                            listOf(
                                kravgrunnlagPeriode(1.januar til 31.januar),
                                kravgrunnlagPeriode(1.februar til 28.februar),
                            ),
                    ),
                ),
            )
        vilkårsvurderderingsteg.vurder(
            1.januar til 31.januar,
            Vilkårsvurderderingsteg.Vurdering.GodTro(
                beløpIBehold = Vilkårsvurderderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                "Brukeren brukte alt på en tur til Vegas",
            ),
        )
        vilkårsvurderderingsteg.vurder(
            1.februar til 28.februar,
            Vilkårsvurderderingsteg.Vurdering.GodTro(
                beløpIBehold = Vilkårsvurderderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                "Brukeren brukte alt på en tur til Vegas",
            ),
        )

        vilkårsvurderderingsteg.erFullstending() shouldBe true
    }
}
