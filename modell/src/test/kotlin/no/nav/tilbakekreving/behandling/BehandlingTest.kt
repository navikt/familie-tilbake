package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrowWithMessage
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingTest {
    @Test
    fun `sett behandling på vent`() {
        val behandling = behandling()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val periode = 1.januar til 31.januar
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")

        val faktasteg = faktastegVurdering(periode)
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere fakta.") {
            behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler())

        val foreldelse = Foreldelsesteg.Vurdering.IkkeForeldet("Begrunnelse")
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere foreldelse.") {
            behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler())

        val vilkårsvurdering = Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått("Begrunnelse", Vilkårsvurderingsteg.VurdertAktsomhet.Forsett("Begrunnelse", false))
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vilkårsvurdering.") {
            behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler())

        val foreslåVedtak = ForeslåVedtakSteg.Vurdering.ForeslåVedtak(null, listOf(ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst(periode, null, null, null, null, null)))

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vedtaksforslag.") {
            behandling.håndter(ansvarligSaksbehandler, foreslåVedtak, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, foreslåVedtak, BehandlingObservatørOppsamler())

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere behandlingsutfall.") {
            behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent, BehandlingObservatørOppsamler())
    }
}
