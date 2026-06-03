package no.nav.tilbakekreving.tilstand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.assertions.skalHaSteg
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.iverksettelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import org.junit.jupiter.api.Test

class TilBehandlingTest {
    @Test
    fun `behandling kan nullstilles når den er i TilBehandling tilstand`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            håndter(
                beslutterContext(),
                listOf(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                ),
            )
        }

        tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId()).foreldelsesteg.erFullstendig(SystemKlokke) shouldBe true
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            flyttTilbakeTilFakta(saksbehandlerContext())
        }
        tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId()).foreldelsesteg.erFullstendig(SystemKlokke) shouldBe false
    }

    @Test
    fun `behandling kan ikke nullstilles når den ikke er i TilBehandling tilstand`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            håndter(
                beslutterContext(),
                listOf(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
                ),
            )
        }

        tilbakekreving.tilstand shouldNotBe TilBehandling

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
                flyttTilbakeTilFakta(saksbehandlerContext())
            }
        }
        exception.message shouldBe "Kan ikke utføre saksbehandling i ${tilbakekreving.tilstand.tilbakekrevingTilstand}"
    }

    @Test
    fun `behandling kan endres for beslutter etter vedtak er foreslått`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext(), true, BehandlerRolle.BESLUTTER).kanEndres shouldBe true
    }

    @Test
    fun `behandling kan ikke endres for annen saksbehandler etter vedtak er foreslått`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), false, BehandlerRolle.SAKSBEHANDLER).kanEndres shouldBe false
    }

    @Test
    fun `behandling kan ikke endres etter foreslått vedtak for samme saksbehandler`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.SAKSBEHANDLER).kanEndres shouldBe false
    }

    @Test
    fun `sistEndret oppdateres ved saksbehandling`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse)

        val gammeltEndringstidspunkt = tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.SAKSBEHANDLER).endretTidspunkt

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            håndter(saksbehandlerContext(), faktastegVurdering())
        }
        val nyttEndringstidspunkt = tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.SAKSBEHANDLER).endretTidspunkt
        nyttEndringstidspunkt shouldNotBe gammeltEndringstidspunkt
    }

    @Test
    fun `svar på behov for fagsysteminfo etter sak har gått til behandling`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(ytelse = Ytelse.Tilleggsstønad),
        )

        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse)
        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 1.januar(2021)),
                ),
            ),
            systemContext(),
        )
        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext())
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext())

        tilbakekreving.tilstand shouldBe TilBehandling

        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.januar(2021) til 1.januar(2021),
                        vedtaksperiode = 1.januar(2021) til 31.januar(2021),
                    ),
                ),
            ),
            systemContext(),
        )

        val perioder = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())
            .vilkårsvurderingsstegDto
            .tilFrontendDto(saksbehandlerContext())
            .perioder
            .map { it.periode }

        perioder shouldBe listOf(1.januar(2021) til 31.januar(2021))
    }

    @Test
    fun `tilbakekreving trekkes tilbake fra godkjenning`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            trekkTilbakeFraGodkjenning(saksbehandlerContext())
        }
        val exception = shouldThrow<ModellFeil> {
            tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
                håndter(beslutterContext(), godkjenning())
            }
        }
        exception.message shouldBe "Behandlingen er i FORESLÅ_VEDTAK og kan ikke behandle vurdering for FATTE_VEDTAK"
    }

    @Test
    fun `beslutter underkjenner vedtak og status endres til utredes for tilbakekreving, og riktig status for behandlingsstegene`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())

        val tilbakekrevingDtoFør = tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.SAKSBEHANDLER)
        tilbakekrevingDtoFør.status shouldBe Behandlingsstatus.FATTER_VEDTAK

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            håndter(
                beslutterContext(),
                listOf(
                    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Fakta må vurderes på nytt"),
                    Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                    Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
                ),
            )
        }

        val tilbakekrevingDtoEtter = tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), false, BehandlerRolle.SAKSBEHANDLER)
        tilbakekrevingDtoEtter.status shouldBe Behandlingsstatus.UTREDES
        tilbakekrevingDtoEtter.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FAKTA).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
        tilbakekrevingDtoEtter.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FORESLÅ_VEDTAK).behandlingsstegstatus shouldBe Behandlingsstegstatus.VENTER
        tilbakekrevingDtoEtter.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FATTE_VEDTAK).behandlingsstegstatus shouldBe Behandlingsstegstatus.VENTER
    }

    @Test
    fun `kan ikke behandles utenfor TilBehandling`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingHendelse, saksbehandlerContext())
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            håndter(saksbehandlerContext(), faktastegVurdering())
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            lagreUttalelse(
                uttalelseVurdering = UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG,
                uttalelseInfo = null,
                kommentar = "Trenger ikke forhåndsvarsel i test lol",
                sideeffektContext = saksbehandlerContext(),
            )
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            håndter(saksbehandlerContext(), 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
            håndter(saksbehandlerContext(), 1.januar(2021) til 31.januar(2021), forårsaketAvNav().burdeForstått())
            håndterForeslåVedtak(saksbehandlerContext())
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            håndter(beslutterContext(), fatteVedtakVurdering())
        }
        tilbakekreving.håndter(iverksettelse(), systemContext())

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.SAKSBEHANDLER).kanEndres shouldBe false

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
                håndter(beslutterContext(), fatteVedtakVurdering())
            }
        }
        exception.melding shouldBe "Kan ikke utføre saksbehandling i JOURNALFØR_VEDTAK"
    }

    private fun tilbakekrevingTilGodkjenning(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        context: SideeffektContext,
    ) = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse).apply {
        gjørSaksbehandling(nåværendeBehandlingId(), context) {
            lagreUttalelse(UttalelseVurdering.JA, null, "", context)
            håndter(context, faktastegVurdering())
            håndter(
                context,
                periode = 1.januar(2021) til 31.januar(2021),
                vurdering = Foreldelsesteg.Vurdering.IkkeForeldet(
                    "Siste utbetaling er innenfor 3 år",
                ),
            )
            håndter(
                context,
                periode = 1.januar(2021) til 31.januar(2021),
                vurdering = forårsaketAvNav().burdeForstått(aktsomhet = uaktsomt(skalIkkeUnnlates(), ingenReduksjon())),
            )
            håndterForeslåVedtak(context)
        }
    }
}
