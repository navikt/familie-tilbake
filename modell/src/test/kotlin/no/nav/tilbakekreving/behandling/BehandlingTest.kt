package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrow
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.til
import org.junit.jupiter.api.Test

class BehandlingTest {
    @Test
    fun `perioder i foreldelse og vilkårsvurdering splittes dersom det blir splitt i foreldelsen`() {
        val behandling =
            behandling(
                kravgrunnlag(
                    listOf(
                        kravgrunnlagPeriode(1.januar til 28.februar),
                    ),
                ),
            )
        behandling.splittForeldetPerioder(listOf(1.januar til 31.januar, 1.februar til 28.februar))
        shouldThrow<NoSuchElementException> {
            behandling.vilkårsvurderingsteg.vurder(
                1.januar til 28.februar,
                Vilkårsvurderingsteg.Vurdering.GodTro(
                    beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                    begrunnelse = "",
                ),
            )
        }

        behandling.vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            Vilkårsvurderingsteg.Vurdering.GodTro(
                beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                begrunnelse = "",
            ),
        )
    }

    @Test
    fun `bare perioder i vilkårsvurdering splittes dersom det blir splitt i vikårsvurderingen`() {
        val behandling =
            behandling(
                kravgrunnlag(
                    listOf(
                        kravgrunnlagPeriode(1.januar til 28.februar),
                    ),
                ),
            )
        behandling.splittVilkårsvurdertePerioder(listOf(1.januar til 31.januar, 1.februar til 28.februar))
        shouldThrow<NoSuchElementException> {
            behandling.foreldelsesteg.vurderForeldelse(
                1.januar til 31.januar,
                Foreldelsesteg.Vurdering.IkkeForeldet(""),
            )
        }

        behandling.foreldelsesteg.vurderForeldelse(
            1.januar til 28.februar,
            Foreldelsesteg.Vurdering.IkkeForeldet(""),
        )
        shouldThrow<NoSuchElementException> {
            behandling.vilkårsvurderingsteg.vurder(
                1.januar til 28.februar,
                Vilkårsvurderingsteg.Vurdering.GodTro(
                    beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                    begrunnelse = "",
                ),
            )
        }

        behandling.vilkårsvurderingsteg.vurder(
            1.januar til 31.januar,
            Vilkårsvurderingsteg.Vurdering.GodTro(
                beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                begrunnelse = "",
            ),
        )
    }
}
