package no.nav.tilbakekreving.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.assertions.skalHaSteg
import no.nav.tilbakekreving.behandlerContext
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
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
        behandling.håndter(saksbehandlerContext(), faktasteg)

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI

        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true)
            .behandlingsstegsinfo.find { it.behandlingssteg == Behandlingssteg.FAKTA }
            .shouldNotBeNull()
            .behandlingsstegstatus shouldBe Behandlingsstegstatus.UTFØRT

        TilBehandling.håndterNullstilling(behandling, Sporing("fefe", "fe"), saksbehandlerContext())

        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.beskrivelse shouldBe null
        behandling.faktastegFrontendDto(Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, LocalDateTime.now()).vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.IKKE_VURDERT
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller foreldelse`() {
        val kravgrunnlag = kravgrunnlag()
        val behandling = behandling(kravgrunnlag)
        behandling.apply {
            lagreUttalelse(UttalelseVurdering.JA, null, null, saksbehandlerContext())
        }

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(saksbehandlerContext(), faktasteg)

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(saksbehandlerContext(), periode, foreldelse)

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.first().begrunnelse shouldBe "Begrunnelse"

        behandling.flyttTilbakeTilFakta(saksbehandlerContext())

        behandling.foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `flytt behandling tilbake til fakta - nullstiller vilkårsvurderingen`() {
        val behandling = behandling()
        behandling.apply {
            lagreUttalelse(UttalelseVurdering.JA, null, null, saksbehandlerContext())
        }

        val faktasteg = faktastegVurdering(periode)
        behandling.håndter(saksbehandlerContext(), faktasteg)

        val foreldelse = Foreldelsesteg.Vurdering.Foreldet("Begrunnelse")
        behandling.håndter(saksbehandlerContext(), periode, foreldelse)

        val vilkårsvurdering = forårsaketAvNav().burdeForstått(aktsomhet = forsettelig())
        behandling.håndter(saksbehandlerContext(), periode, vilkårsvurdering)

        behandling.vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext()).perioder.first().begrunnelse.shouldNotBeNull()

        behandling.flyttTilbakeTilFakta(saksbehandlerContext())

        behandling.vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext()).perioder.first().begrunnelse shouldBe null
    }

    @Test
    fun `vedtak kan endres etter alle tilbakeførte vurderinger er gjennomgått`() {
        val behandling = behandling()
        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.lagreForhåndsvarselUnntak(
            BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            "Trenger ikke forhåndsvarsel i test lol",
            saksbehandlerContext(),
        )
        behandling.håndter(saksbehandlerContext(), periode, foreldelseVurdering())
        behandling.håndter(saksbehandlerContext(), periode, forårsaketAvNav().godTro())
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(saksbehandlerContext())

        behandling.håndter(
            sideeffektContext = beslutterContext(),
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
        )
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe true

        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(saksbehandlerContext())
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe false

        behandling.håndter(
            sideeffektContext = beslutterContext(),
            vurderinger = fatteVedtakVurdering(),
        )
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe false
    }

    @Test
    fun `andre saksbehandlere skal kunne gjøre vurderinger på vedtak som er underkjent`() {
        val behandling = behandling()
        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.lagreForhåndsvarselUnntak(
            BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            "Trenger ikke forhåndsvarsel i test lol",
            saksbehandlerContext(),
        )
        behandling.håndter(saksbehandlerContext(), periode, foreldelseVurdering())
        behandling.håndter(saksbehandlerContext(), periode, forårsaketAvNav().godTro())
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(saksbehandlerContext())

        behandling.håndter(
            sideeffektContext = beslutterContext(),
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
        )
        behandling.tilFrontendDto(TilBehandling, behandlerContext(Behandler.Saksbehandler("Z222222")), true).kanEndres shouldBe true
    }

    @Test
    fun `behandling sendt til godkjenning etter underkjenning skal ikke beholde vurdering`() {
        val behandling = behandling()
        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.lagreForhåndsvarselUnntak(
            BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            "Trenger ikke forhåndsvarsel i test lol",
            saksbehandlerContext(),
        )
        behandling.håndter(saksbehandlerContext(), periode, foreldelseVurdering())
        behandling.håndter(saksbehandlerContext(), periode, forårsaketAvNav().godTro())
        behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true).kanEndres shouldBe true

        behandling.håndterForeslåVedtak(saksbehandlerContext())

        behandling.håndter(
            sideeffektContext = beslutterContext(),
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
        )

        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.håndterForeslåVedtak(saksbehandlerContext())
        val frontendDto = behandling.tilFrontendDto(TilBehandling, saksbehandlerContext(), true)

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
        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.lagreForhåndsvarselUnntak(
            BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            "Trenger ikke forhåndsvarsel i test lol",
            saksbehandlerContext(),
        )
        behandling.håndter(saksbehandlerContext(), periode, foreldelseVurdering())
        behandling.håndter(saksbehandlerContext(), periode, forårsaketAvNav().godTro())
        behandling.håndterForeslåVedtak(saksbehandlerContext())

        behandling.håndter(
            sideeffektContext = beslutterContext(),
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
        )

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            behandling.håndterForeslåVedtak(saksbehandlerContext())
        }
        exception.message shouldBe "Du må gjøre en ny vurdering av fakta før du kan sende vedtaket til godkjenning hos beslutter"
    }

    @Test
    fun `kan ikke sende tilbake til beslutter om en av vurderingene ikke er fullstendige - flere steg`() {
        val behandling = behandling()
        behandling.håndter(saksbehandlerContext(), faktastegVurdering())
        behandling.lagreForhåndsvarselUnntak(
            BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            "Trenger ikke forhåndsvarsel i test lol",
            saksbehandlerContext(),
        )
        behandling.håndter(saksbehandlerContext(), periode, foreldelseVurdering())
        behandling.håndter(saksbehandlerContext(), periode, forårsaketAvNav().godTro())
        behandling.håndterForeslåVedtak(saksbehandlerContext())

        behandling.håndter(
            sideeffektContext = beslutterContext(),
            vurderinger = fatteVedtakVurdering(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Underkjent("Må vurderes på nytt"),
            ),
        )

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            behandling.håndterForeslåVedtak(saksbehandlerContext())
        }
        exception.message shouldBe "Du må gjøre en ny vurdering av fakta, foreldelse og vilkår før du kan sende vedtaket til godkjenning hos beslutter"
    }
}
