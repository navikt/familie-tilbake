package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.assertions.skalHaSteg
import no.nav.tilbakekreving.behandlerContext
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.forsettelig
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingTest {
    val periode = 1.januar(2021) til 31.januar(2021)

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller fakta`() {
        val behandling = behandling()

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT

        val faktasteg = faktastegVurdering(periode)
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktasteg)
            behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
            behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI
            behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER)
                .behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
                .shouldNotBeNull()
                .behandlingsstegstatus shouldBe Behandlingsstegstatus.UTFØRT
            flyttTilbakeTilFakta()
        }

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller foreldelse`() {
        val kravgrunnlag = kravgrunnlag()
        val revurdering = eksternFagsakBehandling()
        val behandling = behandling(kravgrunnlag, revurdering)
        behandling.medSaksbehandling(saksbehandlerContext()) {
            lagreUttalelse(UttalelseVurdering.JA, null, null)
            vurderFakta(faktastegVurdering(periode))
            vurderForeldelse(periode, Foreldelsesteg.Vurdering.Foreldet("Begrunnelse"))
            behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag, revurdering).foreldetPerioder.first().begrunnelse shouldBe "Begrunnelse"
            flyttTilbakeTilFakta()
        }

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag, revurdering).foreldetPerioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller vilkårsvurderingen`() {
        val behandling = behandling()
        behandling.medSaksbehandling(saksbehandlerContext()) {
            lagreUttalelse(UttalelseVurdering.JA, null, null)
            vurderFakta(faktastegVurdering(periode))
            vurderForeldelse(periode, Foreldelsesteg.Vurdering.Foreldet("Begrunnelse"))
            vurderVilkår(periode, forårsaketAvNav().burdeForstått(aktsomhet = forsettelig()))
            behandling.vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext()).perioder.first().begrunnelse.shouldNotBeNull()
            flyttTilbakeTilFakta()
        }

        behandling.vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext()).perioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `vedtak kan endres etter alle tilbakeførte vurderinger er gjennomgått`() {
        val behandling = behandling()
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                "Trenger ikke forhåndsvarsel i test lol",
            )
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvNav().godTro())
            behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true
            foreslåVedtak()
        }

        behandling.medSaksbehandling(beslutterContext()) {
            fatteVedtak(
                fatteVedtakVurdering(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                ),
            )
        }
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true

        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true
            foreslåVedtak()
        }
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe false

        behandling.medSaksbehandling(beslutterContext()) { fatteVedtak(fatteVedtakVurdering()) }
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe false
    }

    @Test
    fun `andre saksbehandlere skal kunne gjøre vurderinger på vedtak som er underkjent`() {
        val behandling = behandling()
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                "Trenger ikke forhåndsvarsel i test lol",
            )
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvNav().godTro())
            behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true
            foreslåVedtak()
        }

        behandling.medSaksbehandling(beslutterContext()) {
            fatteVedtak(
                fatteVedtakVurdering(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                ),
            )
        }
        behandling.tilFrontendDto(TilBehandling, behandlerContext(Behandler.Saksbehandler("Z222222")), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true
    }

    @Test
    fun `behandling sendt til godkjenning etter underkjenning skal ikke beholde vurdering`() {
        val behandling = behandling()
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                "Trenger ikke forhåndsvarsel i test lol",
            )
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvNav().godTro())
            behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true
            foreslåVedtak()
        }

        behandling.medSaksbehandling(beslutterContext()) {
            fatteVedtak(
                fatteVedtakVurdering(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                ),
            )
        }

        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            foreslåVedtak()
        }
        val frontendDto = behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER)

        val fatteVedtakSteg = frontendDto.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FATTE_VEDTAK)
        fatteVedtakSteg.behandlingsstegstatus shouldBe Behandlingsstegstatus.KLAR
        behandling.fatteVedtakStegDto.tilFrontendDto(saksbehandlerContext()).totrinnsstegsinfo shouldBe listOf(
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
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                "Trenger ikke forhåndsvarsel i test lol",
            )
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvNav().godTro())
            foreslåVedtak()
        }

        behandling.medSaksbehandling(beslutterContext()) {
            fatteVedtak(
                fatteVedtakVurdering(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                ),
            )
        }

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            behandling.medSaksbehandling(saksbehandlerContext()) { foreslåVedtak() }
        }
        exception.message shouldBe "Du må gjøre en ny vurdering av fakta før du kan sende vedtaket til godkjenning hos beslutter"
    }

    @Test
    fun `kan ikke sende tilbake til beslutter om en av vurderingene ikke er fullstendige - flere steg`() {
        val behandling = behandling()
        behandling.medSaksbehandling(saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
            lagreForhåndsvarselUnntak(
                BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
                "Trenger ikke forhåndsvarsel i test lol",
            )
            vurderForeldelse(periode, foreldelseVurdering())
            vurderVilkår(periode, forårsaketAvNav().godTro())
            foreslåVedtak()
        }

        behandling.medSaksbehandling(beslutterContext()) {
            fatteVedtak(
                fatteVedtakVurdering(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                    Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                    Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                ),
            )
        }

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            behandling.medSaksbehandling(saksbehandlerContext()) { foreslåVedtak() }
        }
        exception.message shouldBe "Du må gjøre en ny vurdering av fakta, foreldelse og vilkår før du kan sende vedtaket til godkjenning hos beslutter"
    }
}
