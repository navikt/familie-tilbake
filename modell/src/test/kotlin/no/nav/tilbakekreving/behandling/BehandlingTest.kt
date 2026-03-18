package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.forsettelig
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingTest {
    @Test
    fun `sett behandling på vent`() {
        val behandling = behandling()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val periode = 1.januar(2021) til 31.januar(2021)
        val behandlingslogg = Behandlingslogg(listOf<LoggInnslag>().toMutableList())
        behandling.lagreUttalelse(UttalelseVurdering.JA, listOf(), null)
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")

        val faktasteg = faktastegVurdering(periode)
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere fakta.") {
            behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler(), behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler(), behandlingslogg)

        val foreldelse = Foreldelsesteg.Vurdering.IkkeForeldet("Begrunnelse")
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere foreldelse.") {
            behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler(), behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler(), behandlingslogg)

        val vilkårsvurdering = forårsaketAvNav().burdeForstått(aktsomhet = forsettelig())
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vilkårsvurdering.") {
            behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler(), behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler(), behandlingslogg)

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vedtaksforslag.") {
            behandling.håndterForeslåVedtak(
                ansvarligSaksbehandler,
                BehandlingObservatørOppsamler(),
                behandlingslogg,
            )
        }

        behandling.taAvVent()
        behandling.håndterForeslåVedtak(
            ansvarligSaksbehandler,
            BehandlingObservatørOppsamler(),
            behandlingslogg,
        )

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere behandlingsutfall.") {
            behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), listOf(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent), BehandlingObservatørOppsamler(), behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(Behandler.Saksbehandler("Ansvarlig beslutter"), listOf(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent), BehandlingObservatørOppsamler(), behandlingslogg)
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller fakta`() {
        val behandling = behandling()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val periode = 1.januar(2021) til 31.januar(2021)
        val behandlingslogg = Behandlingslogg(listOf<LoggInnslag>().toMutableList())

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler(), behandlingslogg)

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI

        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true)
            .behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
            .shouldNotBeNull()
            .behandlingsstegstatus shouldBe Behandlingsstegstatus.UTFØRT

        TilBehandling.håndterNullstilling(behandling, Sporing("fefe", "fe"), behandlingslogg)

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller foreldelse`() {
        val kravgrunnlag = kravgrunnlag()
        val behandling = behandling(kravgrunnlag)
        behandling.apply {
            lagreUttalelse(UttalelseVurdering.JA, listOf(), null)
        }

        val periode = 1.januar(2021) til 31.januar(2021)
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val behandlingslogg = Behandlingslogg(listOf<LoggInnslag>().toMutableList())

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler(), behandlingslogg)

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler(), behandlingslogg)

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.first().begrunnelse shouldBe "Begrunnelse"

        behandling.flyttTilbakeTilFakta(behandlingslogg)

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller vilkårsvurderingen`() {
        val behandling = behandling()
        behandling.apply {
            lagreUttalelse(UttalelseVurdering.JA, listOf(), null)
        }
        val periode = 1.januar(2021) til 31.januar(2021)
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val behandlingslogg = Behandlingslogg(listOf<LoggInnslag>().toMutableList())

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, BehandlingObservatørOppsamler(), behandlingslogg)

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, BehandlingObservatørOppsamler(), behandlingslogg)

        val vilkårsvurdering = forårsaketAvNav().burdeForstått(aktsomhet = forsettelig())
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, BehandlingObservatørOppsamler(), behandlingslogg)

        behandling.vilkårsvurderingsstegDto.tilFrontendDto().perioder.first().begrunnelse.shouldNotBeNull()

        behandling.flyttTilbakeTilFakta(behandlingslogg)

        behandling.vilkårsvurderingsstegDto.tilFrontendDto().perioder.first().begrunnelse shouldBe null
    }
}
