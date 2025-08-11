package no.nav.tilbakekreving.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.varselbrev
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class IverksettVedtakTest {
    @Test
    fun `iverksett vedtak`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.VILKÅRSVURDERING, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        tilbakekreving.tilstand shouldBe IverksettVedtak
    }

    @Test
    fun `iverksetter ikke vedtak før alle behandlingsstegene er fullført`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        tilbakekreving.tilstand shouldBe TilBehandling
    }

    private fun tilbakekrevingTilGodkjenning(
        oppsamler: BehovObservatørOppsamler,
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
    ) = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingHendelse).apply {
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

    @Test
    fun `tilbakekrevingen er avsluttet når vedtak er iverksatt`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORELDELSE, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.VILKÅRSVURDERING, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                vedtakId = BigInteger("1234"),
            ),
        )
        tilbakekreving.tilstand shouldBe Avsluttet
    }

    @Test
    fun `tilbakekrevingen kan ikke avsluttes når behandlingen er ikke fullført`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FAKTA, FatteVedtakSteg.Vurdering.Godkjent)
        tilbakekreving.håndter(Behandler.Saksbehandler("Z999999"), Behandlingssteg.FORESLÅ_VEDTAK, FatteVedtakSteg.Vurdering.Godkjent)

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                IverksettelseHendelse(
                    iverksattVedtakId = UUID.randomUUID(),
                    vedtakId = BigInteger("1234"),
                ),
            )
        }

        exception.message shouldBe "Forventet ikke IverksettelseHendelse i TIL_BEHANDLING"
        tilbakekreving.tilstand shouldBe TilBehandling
    }
}
