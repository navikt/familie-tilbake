package no.nav.tilbakekreving.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.varselbrev
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class IverksettVedtakTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `iverksett vedtak`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)

        tilbakekreving.håndter(
            beslutter = Behandler.Saksbehandler("Z999999"),
            vurderinger = listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )
        tilbakekreving.tilstand shouldBe IverksettVedtak
    }

    @Test
    fun `iverksetter ikke vedtak før alle behandlingsstegene er fullført`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingEvent)
        tilbakekreving.håndter(
            beslutter = Behandler.Saksbehandler("Z999999"),
            vurderinger = listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )
        tilbakekreving.tilstand shouldBe TilBehandling
    }

    private fun tilbakekrevingTilGodkjenning(
        oppsamler: BehovObservatørOppsamler,
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
    ) = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingHendelse, bigQueryService, EndringObservatørOppsamler()).apply {
        håndter(kravgrunnlag())
        håndter(fagsysteminfoHendelse())
        håndter(brukerinfoHendelse())
        håndter(VarselbrevSendtHendelse(varselbrev()))

        håndter(
            Behandler.Saksbehandler("Ansvarlig saksbehandler"),
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
                aktsomhet = NivåAvForståelse.Aktsomhet.Uaktsomhet(
                    kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
                        ReduksjonSærligeGrunner(
                            begrunnelse = "Jaha",
                            grunner = emptySet(),
                            skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
                        ),
                    ),
                    begrunnelse = "",
                ),
                begrunnelse = "",
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

        tilbakekreving.håndter(
            Behandler.Saksbehandler("Z999999"),
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
            ),
        )

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
        tilbakekreving.håndter(
            Behandler.Saksbehandler("Z999999"),
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
                ),
            )
        }

        exception.message shouldBe "Forventet ikke IverksettelseHendelse i TIL_BEHANDLING"
        tilbakekreving.tilstand shouldBe TilBehandling
    }
}
