package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.prosentReduksjon
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.skalUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import org.junit.jupiter.api.Test

class VilkårsvurderingstegTest {
    @Test
    fun `vilkårsvurdering på en av to perioder`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().godTro(beløpIBehold = null),
        )

        vilkårsvurderingsteg.erFullstendig() shouldBe false
    }

    @Test
    fun `vilkårsvurdering på begge perioder`() {
        val kravgrunnlag = kravgrunnlag(
            perioder =
                listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
            )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().godTro(beløpIBehold = null),
        )
        vilkårsvurderingsteg.erFullstendig() shouldBe false

        vilkårsvurderingsteg.vurder(
            1.februar(2021) til 28.februar(2021),
            forårsaketAvNav().godTro(beløpIBehold = null),
        )

        vilkårsvurderingsteg.erFullstendig() shouldBe true
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr med delvis tilbakekreving`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
            )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().burdeForstått(uaktsomt(skalIkkeUnnlates(), 50.prosentReduksjon)),
        )

        vilkårsvurderingsteg.perioder().first().reduksjon().shouldBeInstanceOf<Reduksjon.Prosentdel>()
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr med ingen tilbakekreving`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
            )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().burdeForstått(uaktsomt(skalUnnlates())),
        )

        vilkårsvurderingsteg.perioder().first().reduksjon().shouldBeInstanceOf<Reduksjon.IngenTilbakekreving>()
    }
}
