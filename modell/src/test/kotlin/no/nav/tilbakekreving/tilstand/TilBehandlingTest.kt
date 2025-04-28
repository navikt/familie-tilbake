package no.nav.tilbakekreving.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingEvent
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.varselbrev
import org.junit.jupiter.api.Test

class TilBehandlingTest {
    @Test
    fun `behanlding kan nullstilles når den er i TilBehandling tilstand`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)

        val behandlingFørNullstilling = tilbakekreving.behandlingHistorikk.nåværende().entry
        tilbakekreving.nullstillBehandling()
        val behandlingEtterNullstilling = tilbakekreving.behandlingHistorikk.nåværende().entry

        behandlingFørNullstilling.foreldelsesteg.erFullstending() shouldBe true
        behandlingEtterNullstilling.foreldelsesteg.erFullstending() shouldBe false
    }

    @Test
    fun `behanlding kan ikke nullstilles når den ikke er i TilBehandling tilstand`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingEvent()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.VILKÅRSVURDERING, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        tilbakekreving.tilstand shouldNotBe TilBehandling

        val exception = shouldThrow<IllegalStateException> {
            tilbakekreving.håndterNullstilling()
        }
        exception.message shouldBe "Forventet ikke Nullstilling i IverksettVedtak"
    }

    private fun tilbakekrevingTilGodkjenning(
        oppsamler: BehovObservatørOppsamler,
        opprettTilbakekrevingEvent: OpprettTilbakekrevingEvent,
    ) = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingEvent).apply {
        håndter(opprettTilbakekrevingEvent)
        håndter(kravgrunnlag())
        håndter(fagsysteminfoHendelse())
        håndter(brukerinfoHendelse())
        håndter(VarselbrevSendtHendelse(varselbrev()))

        håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            FaktaFeilutbetalingsperiodeDto(
                periode = 1.januar til 31.januar,
                hendelsestype = Hendelsestype.INNTEKT,
                hendelsesundertype = Hendelsesundertype.ARBEIDSINNTEKT_FÅTT_INNTEKT,
            ),
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
            vurdering = Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått(
                begrunnelse = "Brukeren gikk opp i lønn",
                aktsomhet = Vilkårsvurderingsteg.VurdertAktsomhet.SimpelUaktsomhet(
                    begrunnelse = "Brukeren gikk opp i lønn og var klar over at det burde føre til en lavere utbetaling.",
                    særligeGrunner = Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner(
                        begrunnelse = "Jaha",
                        grunner = emptySet(),
                    ),
                    skalReduseres = Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei,
                ),
            ),
        )
        håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
            ForeslåVedtakSteg.Vurdering.ForeslåVedtak(
                oppsummeringstekst = null,
                perioderMedTekst = emptyList(),
            ),
        )
    }
}
