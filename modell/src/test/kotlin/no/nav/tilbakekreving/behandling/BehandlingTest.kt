package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.assertions.skalHaSteg
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.foreldelseVurdering
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
    val behandlingObservatør = BehandlingObservatørOppsamler()
    val ansvarligSaksbehandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
    val ansvarligBeslutter = Behandler.Saksbehandler("Ansvarlig beslutter")
    val behandlingslogg = Behandlingslogg(listOf<LoggInnslag>().toMutableList())
    val periode = 1.januar(2021) til 31.januar(2021)

    @Test
    fun `sett behandling på vent`() {
        val behandling = behandling()
        behandling.lagreUttalelse(UttalelseVurdering.JA, listOf(), null)
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")

        val faktasteg = faktastegVurdering(periode)
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere fakta.") {
            behandling.håndter(ansvarligSaksbehandler, faktasteg, behandlingObservatør, behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, faktasteg, behandlingObservatør, behandlingslogg)

        val foreldelse = Foreldelsesteg.Vurdering.IkkeForeldet("Begrunnelse")
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere foreldelse.") {
            behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, behandlingObservatør, behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, behandlingObservatør, behandlingslogg)

        val vilkårsvurdering = forårsaketAvNav().burdeForstått(aktsomhet = forsettelig())
        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vilkårsvurdering.") {
            behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, behandlingObservatør, behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, behandlingObservatør, behandlingslogg)

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere vedtaksforslag.") {
            behandling.håndterForeslåVedtak(
                ansvarligSaksbehandler,
                behandlingObservatør,
                behandlingslogg,
            )
        }

        behandling.taAvVent()
        behandling.håndterForeslåVedtak(
            ansvarligSaksbehandler,
            behandlingObservatør,
            behandlingslogg,
        )

        behandling.settPåVent(Venteårsak.MANGLER_STØTTE, LocalDate.MAX, "Begrunnelse")
        shouldThrowWithMessage<ModellFeil.UgyldigOperasjonException>("Behandling er satt på vent. Kan ikke håndtere behandlingsutfall.") {
            behandling.håndter(ansvarligBeslutter, listOf(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent), behandlingObservatør, behandlingslogg)
        }

        behandling.taAvVent()
        behandling.håndter(ansvarligBeslutter, listOf(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent), behandlingObservatør, behandlingslogg)
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller fakta`() {
        val behandling = behandling()

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, behandlingObservatør, behandlingslogg)

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

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, behandlingObservatør, behandlingslogg)

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, behandlingObservatør, behandlingslogg)

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

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(ansvarligSaksbehandler, faktasteg, behandlingObservatør, behandlingslogg)

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelse, behandlingObservatør, behandlingslogg)

        val vilkårsvurdering = forårsaketAvNav().burdeForstått(aktsomhet = forsettelig())
        behandling.håndter(ansvarligSaksbehandler, periode, vilkårsvurdering, behandlingObservatør, behandlingslogg)

        behandling.vilkårsvurderingsstegDto.tilFrontendDto().perioder.first().begrunnelse.shouldNotBeNull()

        behandling.flyttTilbakeTilFakta(behandlingslogg)

        behandling.vilkårsvurderingsstegDto.tilFrontendDto().perioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `vedtak kan endres etter alle tilbakeførte vurderinger er gjennomgått`() {
        val behandling = behandling()
        behandling.håndter(ansvarligSaksbehandler, faktastegVurdering(), behandlingObservatør, behandlingslogg)
        behandling.lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Trenger ikke forhåndsvarsel i test lol")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelseVurdering(), behandlingObservatør, behandlingslogg)
        behandling.håndter(ansvarligSaksbehandler, periode, forårsaketAvNav().godTro(), behandlingObservatør, behandlingslogg)
        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)

        behandling.håndter(
            beslutter = ansvarligBeslutter,
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
            observatør = behandlingObservatør,
            behandlingslogg = behandlingslogg,
        )
        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true).kanEndres shouldBe true

        behandling.håndter(ansvarligSaksbehandler, faktastegVurdering(), behandlingObservatør, behandlingslogg)
        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)
        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true).kanEndres shouldBe false

        behandling.håndter(
            beslutter = ansvarligBeslutter,
            vurderinger = fatteVedtakVurdering(),
            observatør = behandlingObservatør,
            behandlingslogg = behandlingslogg,
        )
        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true).kanEndres shouldBe false
    }

    @Test
    fun `behandling sendt til godkjenning etter underkjenning skal ikke beholde vurdering`() {
        val behandling = behandling()
        behandling.håndter(ansvarligSaksbehandler, faktastegVurdering(), behandlingObservatør, behandlingslogg)
        behandling.lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Trenger ikke forhåndsvarsel i test lol")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelseVurdering(), behandlingObservatør, behandlingslogg)
        behandling.håndter(ansvarligSaksbehandler, periode, forårsaketAvNav().godTro(), behandlingObservatør, behandlingslogg)
        behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)

        behandling.håndter(
            beslutter = ansvarligBeslutter,
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
            observatør = behandlingObservatør,
            behandlingslogg = behandlingslogg,
        )

        behandling.håndter(ansvarligSaksbehandler, faktastegVurdering(), behandlingObservatør, behandlingslogg)
        behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)
        val frontendDto = behandling.tilFrontendDto(TilBehandling, ansvarligSaksbehandler, true)

        val fatteVedtakSteg = frontendDto.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FATTE_VEDTAK)
        fatteVedtakSteg.behandlingsstegstatus shouldBe Behandlingsstegstatus.KLAR
        behandling.fatteVedtakStegDto.tilFrontendDto().totrinnsstegsinfo shouldBe listOf(
            Totrinnsstegsinfo(
                behandlingssteg = Behandlingssteg.FAKTA,
                godkjent = null,
                begrunnelse = null,
            ),
            Totrinnsstegsinfo(
                behandlingssteg = Behandlingssteg.FORHÅNDSVARSEL,
                godkjent = null,
                begrunnelse = null,
            ),
            Totrinnsstegsinfo(
                behandlingssteg = Behandlingssteg.FORELDELSE,
                godkjent = null,
                begrunnelse = null,
            ),
            Totrinnsstegsinfo(
                behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                godkjent = null,
                begrunnelse = null,
            ),
            Totrinnsstegsinfo(
                behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                godkjent = null,
                begrunnelse = null,
            ),
        )
    }

    @Test
    fun `kan ikke sende tilbake til beslutter om en av vurderingene ikke er fullstendige`() {
        val behandling = behandling()
        behandling.håndter(ansvarligSaksbehandler, faktastegVurdering(), behandlingObservatør, behandlingslogg)
        behandling.lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Trenger ikke forhåndsvarsel i test lol")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelseVurdering(), behandlingObservatør, behandlingslogg)
        behandling.håndter(ansvarligSaksbehandler, periode, forårsaketAvNav().godTro(), behandlingObservatør, behandlingslogg)
        behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)

        behandling.håndter(
            beslutter = ansvarligBeslutter,
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
            observatør = behandlingObservatør,
            behandlingslogg = behandlingslogg,
        )

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)
        }
        exception.message shouldBe "Du må gjøre en ny vurdering av fakta før du kan sende vedtaket til godkjenning hos beslutter"
    }

    @Test
    fun `kan ikke sende tilbake til beslutter om en av vurderingene ikke er fullstendige - flere steg`() {
        val behandling = behandling()
        behandling.håndter(ansvarligSaksbehandler, faktastegVurdering(), behandlingObservatør, behandlingslogg)
        behandling.lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Trenger ikke forhåndsvarsel i test lol")
        behandling.håndter(ansvarligSaksbehandler, periode, foreldelseVurdering(), behandlingObservatør, behandlingslogg)
        behandling.håndter(ansvarligSaksbehandler, periode, forårsaketAvNav().godTro(), behandlingObservatør, behandlingslogg)
        behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)

        behandling.håndter(
            beslutter = ansvarligBeslutter,
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
            observatør = behandlingObservatør,
            behandlingslogg = behandlingslogg,
        )

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            behandling.håndterForeslåVedtak(ansvarligSaksbehandler, behandlingObservatør, behandlingslogg)
        }
        exception.message shouldBe "Du må gjøre en ny vurdering av fakta, foreldelse og vilkår før du kan sende vedtaket til godkjenning hos beslutter"
    }
}
