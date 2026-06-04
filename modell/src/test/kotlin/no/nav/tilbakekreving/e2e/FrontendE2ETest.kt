package no.nav.tilbakekreving.e2e
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class FrontendE2ETest {
    @Test
    fun `status på behandling er basert på steg og tilstand`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behovOppsamler = BehovObservatørOppsamler()
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext(behovObservatør = behovOppsamler),
        )

        tilbakekreving.håndter(kravgrunnlag(), systemContext())
        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext())
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext())

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.UTREDES

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering())
        }
        tilbakekreving.sendVarselbrev(
            tilbakekreving.nåværendeBehandlingId(),
            "Tekst fra saksbehandler",
            saksbehandlerContext(behovObservatør = behovOppsamler),
        )
        val varselbrevId = behovOppsamler.behovListe.filterIsInstance<VarselbrevJournalføringBehov>().first().info.id
        tilbakekreving.håndter(
            VarselbrevJournalføringHendelse(
                varselbrevId = varselbrevId,
                journalpostId = "1234",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )

        tilbakekreving.håndter(
            VarselbrevDistribueringHendelse(
                brevId = varselbrevId,
                journalpostId = "1234",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            lagreUttalelse(UttalelseVurdering.JA, null, null)
            vurderForeldelse(1.januar(2021) til 31.januar(2021), foreldelseVurdering())
            vurderVilkår(1.januar(2021) til 31.januar(2021), forårsaketAvBruker().grovtUaktsomt())
            tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.UTREDES
            foreslåVedtak()
        }
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.FATTER_VEDTAK

        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            fatteVedtak(godkjenning())
        }
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK

        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                vedtakId = BigInteger.ZERO,
                behandlingId = UUID.randomUUID(),
            ),
            systemContext(behovObservatør = behovOppsamler),
        )
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.JOURNALFØR_VEDTAK
        tilbakekreving.håndter(
            JournalføringHendelse(
                brevId = (behovOppsamler.behovListe.last() as VedtaksbrevJournalføringBehov).brevId,
                journalpostId = "123",
                dokumentInfoId = "321",
                behandlingId = UUID.randomUUID(),
                fagsakId = tilbakekreving.eksternFagsak.eksternId,
            ),
            systemContext(behovObservatør = behovOppsamler),
        )
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.DISTRIUBER_VEDTAK
        tilbakekreving.håndter(
            DistribusjonHendelse(
                behandlingId = UUID.randomUUID(),
                brevId = (behovOppsamler.behovListe.last() as VedtaksbrevDistribusjonBehov).brevId,
                fagsakId = tilbakekreving.eksternFagsak.eksternId,
                journalpostId = "123",
                dokumentInfoId = "321",
            ),
            systemContext(),
        )
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(), true, BehandlerRolle.BESLUTTER).status shouldBe Behandlingsstatus.AVSLUTTET
    }
}
