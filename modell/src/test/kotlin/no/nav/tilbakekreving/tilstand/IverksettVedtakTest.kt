package no.nav.tilbakekreving.tilstand
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class IverksettVedtakTest {
    @Test
    fun `iverksett vedtak`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingEvent)

        tilbakekreving.håndter(
            behandlingId = tilbakekreving.nåværendeBehandlingId(),
            sideeffektContext = beslutterContext(),
            vurderinger = listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )
        tilbakekreving.tilstand shouldBe IverksettVedtak
    }

    @Test
    fun `iverksetter ikke vedtak før alle behandlingsstegene er fullført`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(
            behandlingId = tilbakekreving.nåværendeBehandlingId(),
            sideeffektContext = beslutterContext(),
            vurderinger = listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )
        tilbakekreving.tilstand shouldBe TilBehandling
    }

    private fun tilbakekrevingTilGodkjenning(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        endringOppsamler: EndringObservatørOppsamler = EndringObservatørOppsamler(),
    ): Tilbakekreving {
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(endringOppsamler),
        )
        tilbakekreving.apply {
            håndter(kravgrunnlag(), systemContext(endringOppsamler))
            håndter(fagsysteminfoHendelse(), systemContext(endringOppsamler))
            håndter(brukerinfoHendelse(), systemContext(endringOppsamler))
            lagreUttalelse(nåværendeBehandlingId(), UttalelseVurdering.JA, null, "", saksbehandlerContext(endringOppsamler))
            håndter(
                nåværendeBehandlingId(),
                saksbehandlerContext(endringOppsamler),
                faktastegVurdering(),
            )
            håndter(
                nåværendeBehandlingId(),
                saksbehandlerContext(endringOppsamler),
                periode = 1.januar(2021) til 31.januar(2021),
                vurdering = Foreldelsesteg.Vurdering.IkkeForeldet(
                    "Siste utbetaling er innenfor 3 år",
                ),
            )
            håndter(
                nåværendeBehandlingId(),
                saksbehandlerContext(endringOppsamler),
                periode = 1.januar(2021) til 31.januar(2021),
                vurdering = forårsaketAvNav().burdeForstått(aktsomhet = uaktsomt(skalIkkeUnnlates(), ingenReduksjon())),
            )
            håndterForeslåVedtak(nåværendeBehandlingId(), saksbehandlerContext(endringOppsamler))
        }
        return tilbakekreving
    }

    @Test
    fun `tilbakekrevingen er JournalførVedtak når vedtak er iverksatt`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingEvent)

        tilbakekreving.håndter(
            tilbakekreving.nåværendeBehandlingId(),
            beslutterContext(),
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                vedtakId = BigInteger("1234"),
                behandlingId = UUID.randomUUID(),
            ),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe JournalførVedtak
    }

    @Test
    fun `tilbakekrevingen kan ikke avsluttes når behandlingen er ikke fullført`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(opprettTilbakekrevingEvent)
        tilbakekreving.håndter(
            tilbakekreving.nåværendeBehandlingId(),
            beslutterContext(),
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                IverksettelseHendelse(
                    iverksattVedtakId = UUID.randomUUID(),
                    vedtakId = BigInteger("1234"),
                    behandlingId = UUID.randomUUID(),
                ),
                systemContext(),
            )
        }

        exception.message shouldBe "Forventet ikke IverksettelseHendelse i TIL_BEHANDLING"
        tilbakekreving.tilstand shouldBe TilBehandling
    }

    @Test
    fun `fattet vedtak fører til at informasjon om vedtaket blir delt`() {
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val endringObservatørOppsamler = EndringObservatørOppsamler()
        val tilbakekreving = tilbakekrevingTilGodkjenning(
            opprettTilbakekrevingHendelse = opprettTilbakekrevingEvent,
            endringOppsamler = endringObservatørOppsamler,
        )

        endringObservatørOppsamler.vedtakFattetFor(tilbakekreving.nåværendeBehandlingId()).size shouldBe 0

        tilbakekreving.håndter(
            tilbakekreving.nåværendeBehandlingId(),
            beslutterContext(endringObservatørOppsamler),
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

        val vedtakFattet = endringObservatørOppsamler.vedtakFattetFor(tilbakekreving.nåværendeBehandlingId())
        vedtakFattet.size shouldBe 1
        val vedtak = vedtakFattet.single()
        vedtak.vurderteUtbetalinger.size shouldBe 1

        val utbetaling = vedtak.vurderteUtbetalinger.single()
        utbetaling.periode shouldBe (1.januar(2021) til 31.januar(2021))
        utbetaling.vilkårsvurdering.aktsomhetFørUtbetaling shouldBe null
        utbetaling.vilkårsvurdering.aktsomhetEtterUtbetaling shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        utbetaling.vilkårsvurdering.særligeGrunner?.beløpReduseres shouldBe VurdertUtbetaling.JaNeiVurdering.Nei
    }
}
