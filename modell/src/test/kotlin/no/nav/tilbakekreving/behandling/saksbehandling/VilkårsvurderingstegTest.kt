package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.prosent
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.prosentReduksjon
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.skalUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import org.junit.jupiter.api.Test
import java.util.UUID

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

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr, forårsaket av bruker, uaktsomt - gir riktig frontend verdier`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
            ),
        )
        val revurdering = eksternFagsakBehandling()
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag,
        )
        val foreldelsesteg = Foreldelsesteg.opprett(revurdering, kravgrunnlag)
        foreldelsesteg.vurderForeldelse(
            1.januar(2021) til 31.januar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet(
                begrunnelse = "Ikke forelget",
            ),
        )

        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvBruker().uaktsomt(skalUnnlates()),
        )

        vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag, foreldelsesteg).perioder.single() shouldBe VurdertVilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            feilutbetaltBeløp = 2000.kroner,
            hendelsestype = Hendelsestype.ANNET,
            reduserteBeløper = emptyList(),
            aktiviteter = emptyList(),
            vilkårsvurderingsresultatInfo = VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                godTro = null,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    ileggRenter = false,
                    andelTilbakekreves = 0.prosent,
                    beløpTilbakekreves = null,
                    begrunnelse = "",
                    særligeGrunner = null,
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = false,
                    særligeGrunnerBegrunnelse = null,
                ),
            ),
            begrunnelse = "",
            foreldet = false,
        )
    }

    @Test
    fun `underkjenning blir lagret`() {
        val vilkårsvurderingssteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag(),
        )

        vilkårsvurderingssteg.underkjennSteget()

        vilkårsvurderingssteg.tilEntity(UUID.randomUUID()).underkjent shouldBe true
    }
}
