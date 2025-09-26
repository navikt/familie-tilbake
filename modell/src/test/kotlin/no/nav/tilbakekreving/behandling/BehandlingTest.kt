package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BrevmottakerSteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.TilBehandling
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

        val vilkårsvurdering = NivåAvForståelse.BurdeForstått(NivåAvForståelse.Aktsomhet.Forsett("Begrunnelse"), "Begrunnelse")
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vilkårsvurdering.") {
            behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler())

        val foreslåVedtak = ForeslåVedtakSteg.Vurdering.ForeslåVedtak(
            null,
            listOf(ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst(periode, null, null, null, null, null)),
        )

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vedtaksforslag.") {
            behandling.håndter(ansvarligSaksbehandler, foreslåVedtak, BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, foreslåVedtak, BehandlingObservatørOppsamler())

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere behandlingsutfall.") {
            behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), listOf(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent), BehandlingObservatørOppsamler())
        }

        behandling.taAvVent()
        behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), listOf(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent), BehandlingObservatørOppsamler())
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller fakta`() {
        val behandling = behandling().apply {
            brevmottakerSteg = BrevmottakerSteg(false, RegistrertBrevmottaker.DefaultMottaker(navn = "navn", personIdent = "ident"))
        }
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val periode = 1.januar til 31.januar

        behandling.faktastegDto.tilFrontendDto().vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegDto.tilFrontendDto().vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler())

        behandling.faktastegDto.tilFrontendDto().vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegDto.tilFrontendDto().vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI

        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true)
            .behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
            .shouldNotBeNull()
            .behandlingsstegstatus shouldBe Behandlingsstegstatus.UTFØRT

        TilBehandling.håndterNullstilling(behandling, Sporing("fefe", "fe"))

        behandling.faktastegDto.tilFrontendDto().vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegDto.tilFrontendDto().vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller foreldelse`() {
        val kravgrunnlag = kravgrunnlag()
        val behandling = behandling(kravgrunnlag).apply {
            brevmottakerSteg = BrevmottakerSteg(false, RegistrertBrevmottaker.DefaultMottaker(navn = "navn", personIdent = "ident"))
        }

        val periode = 1.januar til 31.januar
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler())

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler())

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.first().begrunnelse shouldBe "Begrunnelse"

        behandling.flyttTilbakeTilFakta()

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller vilkårsvurderingen`() {
        val behandling = behandling().apply {
            brevmottakerSteg = BrevmottakerSteg(false, RegistrertBrevmottaker.DefaultMottaker(navn = "navn", personIdent = "ident"))
        }

        val periode = 1.januar til 31.januar
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler())

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler())

        val vilkårsvurdering = NivåAvForståelse.BurdeForstått(NivåAvForståelse.Aktsomhet.Forsett("vurdering"), "vurdering")
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler())

        behandling.vilkårsvurderingsstegDto.tilFrontendDto().perioder.first().begrunnelse shouldBe "vurdering"

        behandling.flyttTilbakeTilFakta()

        behandling.vilkårsvurderingsstegDto.tilFrontendDto().perioder.first().begrunnelse shouldBe null
    }
}
