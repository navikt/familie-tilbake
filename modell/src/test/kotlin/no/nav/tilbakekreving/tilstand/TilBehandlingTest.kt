package no.nav.tilbakekreving.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tilbakekreving.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
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
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
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
                    kravgrunnlagPeriode(1.januar til 1.januar),
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
                        kravgrunnlagPeriode = 1.januar til 1.januar,
                        vedtaksperiode = 1.januar til 31.januar,
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

        perioder shouldBe listOf(1.januar til 31.januar)
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
            periode = 1.januar til 31.januar,
            vurdering = Foreldelsesteg.Vurdering.IkkeForeldet(
                "Siste utbetaling er innenfor 3 år",
            ),
        )
        håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            periode = 1.januar til 31.januar,
            vurdering = NivåAvForståelse.BurdeForstått(
                begrunnelse = "Brukeren gikk opp i lønn",
                aktsomhet = NivåAvForståelse.Aktsomhet.Uaktsomhet(
                    begrunnelse = "Brukeren gikk opp i lønn og var klar over at det burde føre til en lavere utbetaling.",
                    kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
                        reduksjonSærligeGrunner = ReduksjonSærligeGrunner(
                            begrunnelse = "",
                            grunner = emptySet(),
                            skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
                        ),
                    ),
                ),
            ),
        )
        håndterForeslåVedtak(Behandler.Saksbehandler("Ansvarlig saksbehandler"))
    }
}
