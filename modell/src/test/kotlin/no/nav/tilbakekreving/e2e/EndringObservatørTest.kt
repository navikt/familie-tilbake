package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ANSVARLIG_BESLUTTER
import no.nav.tilbakekreving.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.FagsystemToggle
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
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
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
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
        endringObservatør.statusoppdateringerFor(tilbakekreving.nåværendeBehandlingId()) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
                behandlingstatus = Behandlingsstatus.OPPRETTET,
                totalFeilutbetaltPeriode = 1.januar(2021) til 31.januar(2021),
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
                behandlingstatus = Behandlingsstatus.UTREDES,
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), BehovObservatørOppsamler(), opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
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
        endringObservatør.statusoppdateringerFor(tilbakekreving.nåværendeBehandlingId()) shouldBe listOf(
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = Behandler.Vedtaksløsning.ident,
                vedtaksresultat = null,
                behandlingstatus = Behandlingsstatus.OPPRETTET,
                totalFeilutbetaltPeriode = 1.januar(2021) til 31.januar(2021),
            ),
            EndringObservatørOppsamler.Statusoppdatering(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER.ident,
                vedtaksresultat = null,
                behandlingstatus = Behandlingsstatus.UTREDES,
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.lagreUttalelse(UttalelseVurdering.JA, null, "", ANSVARLIG_SAKSBEHANDLER)
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, null, "", ANSVARLIG_SAKSBEHANDLER)
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())
        tilbakekreving.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER)

        tilbakekreving.håndter(ANSVARLIG_BESLUTTER, godkjenning())
        tilbakekreving.håndter(iverksettelse())
        tilbakekreving.håndter(journalføring((behovOppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId, tilbakekreving.eksternFagsak.eksternId))
        tilbakekreving.håndter(distribusjon((behovOppsamler.behovListe.last() as VedtaksbrevDistribusjonBehov).brevId, tilbakekreving.eksternFagsak.eksternId))
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.lagreUttalelse(UttalelseVurdering.JA, null, "", ANSVARLIG_SAKSBEHANDLER)
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )

        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, null, "", ANSVARLIG_SAKSBEHANDLER)
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), foreldelseVurdering())
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, 1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())
        tilbakekreving.håndterForeslåVedtak(ANSVARLIG_SAKSBEHANDLER)

        endringObservatør.behandlingEndretEventsFor(fagsakId).last().forrigeBehandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
        endringObservatør.behandlingEndretEventsFor(fagsakId).last().behandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_GODKJENNING

        tilbakekreving.håndter(ANSVARLIG_BESLUTTER, godkjenning())
        tilbakekreving.håndter(iverksettelse())
        tilbakekreving.håndter(journalføring((behovOppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId, fagsakId = tilbakekreving.eksternFagsak.eksternId))
        tilbakekreving.håndter(distribusjon((behovOppsamler.behovListe.last() as VedtaksbrevDistribusjonBehov).brevId, fagsakId = tilbakekreving.eksternFagsak.eksternId))

        endringObservatør.behandlingEndretEventsFor(fagsakId).last().forrigeBehandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_GODKJENNING
        endringObservatør.behandlingEndretEventsFor(fagsakId).last().behandlingsstatus shouldBe ForenkletBehandlingsstatus.AVSLUTTET
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
        val tilbakekreving = Tilbakekreving.opprett(UUID.randomUUID().toString(), behovOppsamler, opprettTilbakekrevingHendelse, bigQueryService, endringObservatør, features = defaultFeatures())
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.lagreUttalelse(UttalelseVurdering.JA, null, "", ANSVARLIG_SAKSBEHANDLER)
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())
        tilbakekreving.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, null, "", ANSVARLIG_SAKSBEHANDLER)
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

        endringObservatør.behandlingEndretEventsFor(fagsakId).last().forrigeBehandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_GODKJENNING
        endringObservatør.behandlingEndretEventsFor(fagsakId).last().behandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
    }

    @Test
    fun `sender endringer gjennom saksbehandlingsløp med info om varselbrev`() {
        val behovOppsamler = BehovObservatørOppsamler()
        val fagsakId = "123456"
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(
            eksternFagsak = eksternFagsak(
                eksternId = fagsakId,
                ytelse = Ytelse.Tilleggsstønad,
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            behovObservatør = behovOppsamler,
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            bigQueryService = bigQueryService,
            endringObservatør = endringObservatør,
            features = defaultFeatures(
                fagsystemToggleOverrides = arrayOf(
                    FagsystemToggle.ForhaandsvarselBehandlingsstatuser to true,
                ),
            ),
        )
        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL,
        )
        endringObservatør.behandlingEndretEventsFor(fagsakId).single { it.behandlingsstatus == ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL }.should {
            it.forrigeBehandlingsstatus shouldBe ForenkletBehandlingsstatus.OPPRETTET
        }

        tilbakekreving.håndter(ANSVARLIG_SAKSBEHANDLER, faktastegVurdering())

        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL,
            ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL,
        )
        endringObservatør.behandlingEndretEventsFor(fagsakId).last { it.behandlingsstatus == ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL }.should {
            it.forrigeBehandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL
        }

        tilbakekreving.sendVarselbrev("Sample text")
        tilbakekreving.håndter(behovOppsamler.journalføringEventFor())
        tilbakekreving.håndter(behovOppsamler.distribuerHendelseFor())
        endringObservatør.behandlingEndretEventsFor(fagsakId).map { it.behandlingsstatus } shouldBe listOf(
            ForenkletBehandlingsstatus.OPPRETTET,
            ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL,
            ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL,
            ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL,
            ForenkletBehandlingsstatus.TIL_BEHANDLING,
        )
        endringObservatør.behandlingEndretEventsFor(fagsakId).last { it.behandlingsstatus == ForenkletBehandlingsstatus.TIL_BEHANDLING }.should {
            it.forrigeBehandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_FORHÅNDSVARSEL
            it.behandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
            it.venter.shouldNotBeNull {
                grunn shouldBe Venter.Grunn.BRUKERUTTALELSE
                frist shouldBe LocalDate.now().plusWeeks(3)
            }
        }
    }
}
