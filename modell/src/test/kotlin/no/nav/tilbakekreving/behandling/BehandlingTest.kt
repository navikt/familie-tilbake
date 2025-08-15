package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingTest {
    @Test
    fun `perioder i foreldelse og vilkårsvurdering splittes dersom det blir splitt i foreldelsen`() {
        val behandling =
            behandling(
                kravgrunnlag(
                    perioder = listOf(
                        kravgrunnlagPeriode(1.januar til 28.februar),
                    ),
                ),
            )
        behandling.splittForeldetPerioder(listOf(1.januar til 31.januar, 1.februar til 28.februar))
        shouldThrow<NoSuchElementException> {
            behandling.håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                1.januar til 28.februar,
                Vilkårsvurderingsteg.Vurdering.GodTro(
                    beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                    begrunnelse = "",
                ),
            )
        }

        behandling.håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
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
                    perioder = listOf(
                        kravgrunnlagPeriode(1.januar til 28.februar),
                    ),
                ),
            )
        behandling.splittVilkårsvurdertePerioder(listOf(1.januar til 31.januar, 1.februar til 28.februar))
        shouldThrow<NoSuchElementException> {
            behandling.håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                1.januar til 31.januar,
                Foreldelsesteg.Vurdering.IkkeForeldet(""),
            )
        }

        behandling.håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            1.januar til 28.februar,
            Foreldelsesteg.Vurdering.IkkeForeldet(""),
        )
        shouldThrow<NoSuchElementException> {
            behandling.håndter(
                Behandler.Saksbehandler("Ansvarlig saksbehandler"),
                1.januar til 28.februar,
                Vilkårsvurderingsteg.Vurdering.GodTro(
                    beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                    begrunnelse = "",
                ),
            )
        }

        behandling.håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            1.januar til 31.januar,
            Vilkårsvurderingsteg.Vurdering.GodTro(
                beløpIBehold = Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei,
                begrunnelse = "",
            ),
        )
    }

    @Test
    fun `sett behandling på vent`() {
        val behandling = behandling()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val periode = 1.januar til 31.januar
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")

        val faktasteg = faktastegVurdering(periode)
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere fakta.") {
            behandling.håndter(ansvarligSaksbehandler, faktasteg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, faktasteg)

        val foreldelse = Foreldelsesteg.Vurdering.IkkeForeldet("Begrunnelse")
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere foreldelse.") {
            behandling.håndter(ansvarligSaksbehandler, periode, foreldelse)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse)

        val vilkårsvurdering = Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått("Begrunnelse", Vilkårsvurderingsteg.VurdertAktsomhet.Forsett("Begrunnelse", false))
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vilkårsvurdering.") {
            behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering)

        val foreslåVedtak = ForeslåVedtakSteg.Vurdering.ForeslåVedtak(null, listOf(ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst(periode, null, null, null, null, null)))

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vedtaksforslag.") {
            behandling.håndter(ansvarligSaksbehandler, foreslåVedtak)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, foreslåVedtak)

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere behandlingsutfall.") {
            behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        }

        behandling.taAvVent()
        behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
    }
}
