package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import org.junit.jupiter.api.Test

class VilkårsvurderingstegTest {
    @Test
    fun `vilkårsvurdering på en av to perioder`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar til 31.januar),
                kravgrunnlagPeriode(1.februar til 28.februar),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
            Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag),
        )
        vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            NivåAvForståelse.GodTro(
                begrunnelse = "Brukeren fikk penger som de ikke hadde krav på",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
                begrunnelseForGodTro = "Brukeren brukte alt på en tur til Vegas",
            ),
        )

        vilkårsvurderingsteg.erFullstendig() shouldBe false
    }

    @Test
    fun `vilkårsvurdering på begge perioder`() {
        val kravgrunnlag = kravgrunnlag(
            perioder =
                listOf(
                    kravgrunnlagPeriode(1.januar til 31.januar),
                    kravgrunnlagPeriode(1.februar til 28.februar),
                ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
                foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag),
            )
        vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            NivåAvForståelse.GodTro(
                begrunnelse = "Brukeren fikk penger som de ikke hadde krav på",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
                begrunnelseForGodTro = "Brukeren brukte alt på en tur til Vegas",
            ),
        )
        vilkårsvurderingsteg.erFullstendig() shouldBe false

        vilkårsvurderingsteg.vurder(
            1.februar til 28.februar,
            NivåAvForståelse.GodTro(
                begrunnelse = "Brukeren fikk penger som de ikke hadde krav på",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
                begrunnelseForGodTro = "Brukeren brukte alt på en tur til Vegas",
            ),
        )

        vilkårsvurderingsteg.erFullstendig() shouldBe true
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr med delvis tilbakekreving`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar til 31.januar),
                kravgrunnlagPeriode(1.februar til 28.februar),
            ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
                foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag),
            )
        vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            NivåAvForståelse.BurdeForstått(
                begrunnelse = "Brukeren brukte alt på en tur til Vegas",
                aktsomhet = NivåAvForståelse.Aktsomhet.Uaktsomhet(
                    begrunnelse = "Begrunnelse",
                    kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
                        ReduksjonSærligeGrunner(
                            begrunnelse = "begrunnelse til ReduksjonSærligeGrunner",
                            skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Ja(50),
                            grunner = setOf(),
                        ),
                    ),
                ),
            ),
        )

        vilkårsvurderingsteg.perioder().first().reduksjon().shouldBeInstanceOf<Reduksjon.Prosentdel>()
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr med ingen tilbakekreving`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar til 31.januar),
                kravgrunnlagPeriode(1.februar til 28.februar),
            ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
                foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag),
            )
        vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            NivåAvForståelse.BurdeForstått(
                begrunnelse = "Brukeren brukte alt på en tur til Vegas",
                aktsomhet = NivåAvForståelse.Aktsomhet.Uaktsomhet(
                    begrunnelse = "Begrunnelse",
                    kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.Unnlates,
                ),
            ),
        )

        vilkårsvurderingsteg.perioder().first().reduksjon().shouldBeInstanceOf<Reduksjon.IngenTilbakekreving>()
    }
}
