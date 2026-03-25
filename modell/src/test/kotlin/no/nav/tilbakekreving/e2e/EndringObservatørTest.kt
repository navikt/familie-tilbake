package no.nav.tilbakekreving.e2e

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.distribusjon
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.iverksettelse
import no.nav.tilbakekreving.journalføring
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class EndringObservatørTest {
    private val bigQueryService = BigQueryServiceStub()
    private val endringObservatør = EndringObservatørOppsamler()

    @Test
    fun `vurdering fra saksbehandler fører til at informasjon om endring blir delt`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), BehovObservatørOppsamler(), opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures(Toggle.Vedtaksbrev to true))
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        endringObservatør.statusoppdateringerFor(tilbakekreving.behandlingHistorikk.nåværende().entry.id) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar(2021) til 31.januar(2021),
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar(2021) til 31.januar(2021),
            ),
        )
    }

    @Test
    fun `perioder utvidet med informasjon fra fagsystem blir delt`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), BehovObservatørOppsamler(), opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures(Toggle.Vedtaksbrev to true))
        tilbakekreving.håndter(kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(1.januar(2021) til 1.januar(2021)))))
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
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        endringObservatør.statusoppdateringerFor(tilbakekreving.behandlingHistorikk.nåværende().entry.id) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar(2021) til 31.januar(2021),
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar(2021) til 31.januar(2021),
            ),
        )
    }

    @Test
    fun `sender endringer gjennom saksbehandlingsløp`() {
        val behovOppsamler = BehovObservatørOppsamler()
        val fagsakId = "123456"
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                eksternId = fagsakId,
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures(Toggle.Vedtaksbrev to true))
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, listOf(), "")
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())
        tilbakekreving.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER)

        tilbakekreving.håndter(ANSVARLIG_BESLUTTER, godkjenning())
        tilbakekreving.håndter(iverksettelse())
        tilbakekreving.håndter(journalføring((behovOppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId))
        tilbakekreving.håndter(distribusjon())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_GODKJENNING,
            ForenkletBehandlingsstatus.AVSLUTTET,
        )
    }

    @Test
    fun `behandlingsstatus skal oppdateres til TIL_GODKJENNING når behandling sendes til godkjenning og AVSLUTTET når det godkjennes`() {
        val behovOppsamler = BehovObservatørOppsamler()
        val fagsakId = "123456"
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                eksternId = fagsakId,
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures(Toggle.Vedtaksbrev to true))
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, listOf(), "")
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())
        tilbakekreving.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER)

        endringObservatør.behandlingEndretEventsFor(fagsakId).last().forrigeStatus shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
        endringObservatør.behandlingEndretEventsFor(fagsakId).last().status shouldBe ForenkletBehandlingsstatus.TIL_GODKJENNING

        tilbakekreving.håndter(ANSVARLIG_BESLUTTER, godkjenning())
        tilbakekreving.håndter(iverksettelse())
        tilbakekreving.håndter(journalføring(tilbakekreving.brevHistorikk.nåværende().entry.id))
        tilbakekreving.håndter(distribusjon())

        endringObservatør.behandlingEndretEventsFor(fagsakId).last().forrigeStatus shouldBe ForenkletBehandlingsstatus.TIL_GODKJENNING
        endringObservatør.behandlingEndretEventsFor(fagsakId).last().status shouldBe ForenkletBehandlingsstatus.AVSLUTTET
    }

    @Test
    fun `behandlingsstatus skal oppdateres til TIL_BEHANDLING når vedtaket ikke godkjennes`() {
        val behovOppsamler = BehovObservatørOppsamler()
        val fagsakId = "123456"
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                eksternId = fagsakId,
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures(Toggle.Vedtaksbrev to true))
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, listOf(), "")
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())
        tilbakekreving.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER)

        tilbakekreving.håndter(
            ANSVARLIG_BESLUTTER,
            listOf(
                Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Ikke godkjent"),
                Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Underkjent("Ikke godkjent"),
                Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Underkjent("Ikke godkjent"),
                Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Underkjent("Ikke godkjent"),
                Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Underkjent("Ikke godkjent"),
            ),
        )

        endringObservatør.behandlingEndretEventsFor(fagsakId).last().forrigeStatus shouldBe ForenkletBehandlingsstatus.TIL_GODKJENNING
        endringObservatør.behandlingEndretEventsFor(fagsakId).last().status shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
    }
}
