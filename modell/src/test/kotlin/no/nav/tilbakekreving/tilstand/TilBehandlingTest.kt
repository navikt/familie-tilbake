package no.nav.tilbakekreving.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tilbakekreving.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.assertions.skalHaSteg
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import org.junit.jupiter.api.Test

class TilBehandlingTest {
    @Test
    fun `behandling kan nullstilles når den er i TilBehandling tilstand`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, Behandler.Saksbehandler("Ansvarlig saksbehandler"))

        tilbakekreving.håndter(
            Behandler.Saksbehandler("Z999999"),
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

        tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsesteg.erFullstendig() shouldBe true
        tilbakekreving.håndterNullstilling()
        tilbakekreving.behandlingHistorikk.nåværende().entry.foreldelsesteg.erFullstendig() shouldBe false
    }

    @Test
    fun `behandling kan ikke nullstilles når den ikke er i TilBehandling tilstand`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, Behandler.Saksbehandler("Ansvarlig saksbehandler"))

        tilbakekreving.håndter(
            Behandler.Saksbehandler("Z999999"),
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

        tilbakekreving.tilstand shouldNotBe TilBehandling

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndterNullstilling()
        }
        exception.message shouldBe "Kan ikke flytte tilbake til fakta i ${tilbakekreving.tilstand.tilbakekrevingTilstand}"
    }

    @Test
    fun `behandling kan endres for beslutter etter vedtak er foreslått`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, Behandler.Saksbehandler("Ansvarlig saksbehandler"))

        tilbakekreving.frontendDtoForBehandling(Behandler.Saksbehandler("Ansvarlig beslutter"), true).kanEndres shouldBe true
    }

    @Test
    fun `behandling kan ikke endres for annen saksbehandler etter vedtak er foreslått`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, Behandler.Saksbehandler("Ansvarlig saksbehandler"))

        tilbakekreving.frontendDtoForBehandling(Behandler.Saksbehandler("Annen saksbehandler"), false).kanEndres shouldBe false
    }

    @Test
    fun `behandling kan ikke endres etter foreslått vedtak for samme saksbehandler`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, behandler)

        tilbakekreving.frontendDtoForBehandling(behandler, true).kanEndres shouldBe false
    }

    @Test
    fun `sistEndret oppdateres ved saksbehandling`() {
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilBehandling(oppsamler, opprettTilbakekrevingHendelse)

        val gammeltEndringstidspunkt = tilbakekreving.frontendDtoForBehandling(behandler, true).endretTidspunkt

        tilbakekreving.håndter(behandler, faktastegVurdering())
        val nyttEndringstidspunkt = tilbakekreving.frontendDtoForBehandling(behandler, true).endretTidspunkt
        nyttEndringstidspunkt shouldNotBe gammeltEndringstidspunkt
    }

    @Test
    fun `svar på behov for fagsysteminfo etter sak har gått til behandling`() {
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(ytelse = Ytelse.Tilleggsstønad),
        )

        val tilbakekreving = opprettTilbakekreving(oppsamler, opprettTilbakekrevingHendelse)
        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 1.januar(2021)),
                ),
            ),
        )
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

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
        )

        val perioder = tilbakekreving.behandlingHistorikk
            .nåværende()
            .entry
            .vilkårsvurderingsstegDto
            .tilFrontendDto()
            .perioder
            .map { it.periode }

        perioder shouldBe listOf(1.januar(2021) til 31.januar(2021))
    }

    @Test
    fun `tilbakekreving trekkes tilbake fra godkjenning`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, ANSVARLIG_SAKSBEHANDLER)
        tilbakekreving.håndterTrekkTilbakeFraGodkjenning()
        val exception = shouldThrow<ModellFeil> {
            tilbakekreving.håndter(ANSVARLIG_BESLUTTER, godkjenning())
        }
        exception.message shouldBe "Behandlingen er i FORESLÅ_VEDTAK og kan ikke behandle vurdering for FATTE_VEDTAK"
    }

    @Test
    fun `beslutter underkjenner vedtak og status endres til utredes for tilbakekreving, og riktig status for behandlingsstegene`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, ANSVARLIG_SAKSBEHANDLER)

        val tilbakekrevingDtoFør = tilbakekreving.frontendDtoForBehandling(ANSVARLIG_BESLUTTER, true)
        tilbakekrevingDtoFør.status shouldBe Behandlingsstatus.FATTER_VEDTAK

        tilbakekreving.håndter(
            ANSVARLIG_BESLUTTER,
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Fakta må vurderes på nytt"),
                Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

        val tilbakekrevingDtoEtter = tilbakekreving.frontendDtoForBehandling(ANSVARLIG_SAKSBEHANDLER, false)
        tilbakekrevingDtoEtter.status shouldBe Behandlingsstatus.UTREDES
        tilbakekrevingDtoEtter.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FAKTA).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
        tilbakekrevingDtoEtter.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FORESLÅ_VEDTAK).behandlingsstegstatus shouldBe Behandlingsstegstatus.VENTER
        tilbakekrevingDtoEtter.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FATTE_VEDTAK).behandlingsstegstatus shouldBe Behandlingsstegstatus.VENTER
    }

    private fun tilbakekrevingTilGodkjenning(
        oppsamler: BehovObservatørOppsamler,
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        behandler: Behandler,
    ) = tilbakekrevingTilBehandling(oppsamler, opprettTilbakekrevingHendelse).apply {
        behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")
        håndter(
            behandler,
            faktastegVurdering(),
        )
        håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            periode = 1.januar(2021) til 31.januar(2021),
            vurdering = Foreldelsesteg.Vurdering.IkkeForeldet(
                "Siste utbetaling er innenfor 3 år",
            ),
        )
        håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            periode = 1.januar(2021) til 31.januar(2021),
            vurdering = forårsaketAvNav().burdeForstått(aktsomhet = uaktsomt(skalIkkeUnnlates(), ingenReduksjon())),
        )
        håndterForeslåVedtak(Behandler.Saksbehandler("Ansvarlig saksbehandler"))
    }
}
