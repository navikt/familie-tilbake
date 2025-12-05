package no.nav.tilbakekreving.e2e

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.eksternFagsak
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.forårsaketAvBrukerGrovtUaktsomt
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.iverksettelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.varselbrevHendelse
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), BehovObservatørOppsamler(), opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        endringObservatør.statusoppdateringerFor(tilbakekreving.behandlingHistorikk.nåværende().entry.id) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar til 31.januar,
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar til 31.januar,
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), BehovObservatørOppsamler(), opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(1.januar til 1.januar))))
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
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        endringObservatør.statusoppdateringerFor(tilbakekreving.behandlingHistorikk.nåværende().entry.id) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar til 31.januar,
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
                totalFeilutbetaltPeriode = 1.januar til 31.januar,
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.håndter(varselbrevHendelse(behovOppsamler.sisteVarselbrevId()))
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar til 31.januar, foreldelseVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar til 31.januar, forårsaketAvBrukerGrovtUaktsomt())
        tilbakekreving.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER)

        tilbakekreving.håndter(ANSVARLIG_BESLUTTER, godkjenning())
        tilbakekreving.håndter(iverksettelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.status } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.AVSLUTTET,
        )
    }
}
