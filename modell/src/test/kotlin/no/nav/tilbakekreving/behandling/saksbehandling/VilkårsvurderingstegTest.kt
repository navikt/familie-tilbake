package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.HistorikkStub
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import org.junit.jupiter.api.Test

class VilkårsvurderingstegTest {
    @Test
    fun `vilkårsvurdering på en av to perioder`() {
        val kravgrunnlag =
            HistorikkStub.fakeReferanse(
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar til 31.januar),
                            kravgrunnlagPeriode(1.februar til 28.februar),
                        ),
                ),
            )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                kravgrunnlag,
                Foreldelsesteg.opprett(kravgrunnlag),
            )
        vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            NivåAvForståelse.GodTro(
                begrunnelse = "Brukeren brukte alt på en tur til Vegas",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
            ),
        )

        vilkårsvurderingsteg.erFullstendig() shouldBe false
    }

    @Test
    fun `vilkårsvurdering på begge perioder`() {
        val kravgrunnlag =
            HistorikkStub.fakeReferanse(
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar til 31.januar),
                            kravgrunnlagPeriode(1.februar til 28.februar),
                        ),
                ),
            )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                kravgrunnlagHendelse = kravgrunnlag,
                foreldelsesteg = Foreldelsesteg.opprett(kravgrunnlag),
            )
        vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            NivåAvForståelse.GodTro(
                begrunnelse = "Brukeren brukte alt på en tur til Vegas",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
            ),
        )
        vilkårsvurderingsteg.erFullstendig() shouldBe false

        vilkårsvurderingsteg.vurder(
            1.februar til 28.februar,
            NivåAvForståelse.GodTro(
                begrunnelse = "Brukeren brukte alt på en tur til Vegas",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
            ),
        )

        vilkårsvurderingsteg.erFullstendig() shouldBe true
    }
}
