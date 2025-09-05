package no.nav.tilbakekreving.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

class TilBehandlingTest {
    private val bigQueryService = BigQueryServiceStub()

    @Test
    fun `behanlding kan nullstilles når den er i TilBehandling tilstand`() {
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

        val behandlingFørNullstilling = tilbakekreving.behandlingHistorikk.nåværende().entry
        tilbakekreving.nullstillBehandling()
        val behandlingEtterNullstilling = tilbakekreving.behandlingHistorikk.nåværende().entry

        behandlingFørNullstilling.foreldelsesteg.erFullstendig() shouldBe true
        behandlingEtterNullstilling.foreldelsesteg.erFullstendig() shouldBe false
    }

    @Test
    fun `behanlding kan ikke nullstilles når den ikke er i TilBehandling tilstand`() {
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
        exception.message shouldBe "Forventet ikke Nullstilling i ${tilbakekreving.tilstand.tilbakekrevingTilstand}"
    }

    @Test
    fun `behandling kan endres for beslutter etter vedtak er foreslått`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, Behandler.Saksbehandler("Ansvarlig saksbehandler"))

        tilbakekreving.behandlingHistorikk.nåværende().entry.tilFrontendDto(Behandler.Saksbehandler("Ansvarlig beslutter"), true).kanEndres shouldBe true
    }

    @Test
    fun `behandling kan ikke endres for annen saksbehandler etter vedtak er foreslått`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, Behandler.Saksbehandler("Ansvarlig saksbehandler"))

        tilbakekreving.behandlingHistorikk.nåværende().entry.tilFrontendDto(Behandler.Saksbehandler("Annen saksbehandler"), false).kanEndres shouldBe false
    }

    @Test
    fun `behandling kan ikke endres etter foreslått vedtak for samme saksbehandler`() {
        val oppsamler = BehovObservatørOppsamler()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val tilbakekreving = tilbakekrevingTilGodkjenning(oppsamler, opprettTilbakekrevingHendelse, behandler)

        tilbakekreving.behandlingHistorikk.nåværende().entry.tilFrontendDto(behandler, true).kanEndres shouldBe false
    }

    private fun tilbakekrevingTilGodkjenning(
        oppsamler: BehovObservatørOppsamler,
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        behandler: Behandler,
    ) = Tilbakekreving.opprett(oppsamler, opprettTilbakekrevingHendelse, bigQueryService, EndringObservatørOppsamler()).apply {
        håndter(kravgrunnlag())
        håndter(fagsysteminfoHendelse())
        håndter(brukerinfoHendelse())
        håndter(VarselbrevSendtHendelse(varselbrev()))

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
                    kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.Tilbakekreves(
                        reduksjonSærligeGrunner = ReduksjonSærligeGrunner(
                            begrunnelse = "",
                            grunner = emptySet(),
                            skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
                        ),
                    ),
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
