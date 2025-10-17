package no.nav.tilbakekreving.e2e

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.forårsaketAvBrukerGrovtUaktsomt
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class FrontendE2ETest {
    @Test
    fun `status på behandling er basert på steg og tilstand`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")
        val beslutter = Behandler.Saksbehandler("Ansvarlig beslutter")

        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse,
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
        )

        tilbakekreving.håndter(kravgrunnlag())
        tilbakekreving.håndter(fagsysteminfoHendelse())
        tilbakekreving.håndter(brukerinfoHendelse())

        tilbakekreving.frontendDtoForBehandling(behandler, true).status shouldBe Behandlingsstatus.OPPRETTET

        tilbakekreving.håndter(VarselbrevSendtHendelse(varselbrevId = tilbakekreving.brevHistorikk.nåværende().entry.id, journalpostId = "1234"))
        tilbakekreving.frontendDtoForBehandling(behandler, true).status shouldBe Behandlingsstatus.UTREDES

        tilbakekreving.håndter(behandler, faktastegVurdering())
        tilbakekreving.håndter(behandler, 1.januar til 31.januar, foreldelseVurdering())
        tilbakekreving.håndter(behandler, 1.januar til 31.januar, forårsaketAvBrukerGrovtUaktsomt())
        tilbakekreving.frontendDtoForBehandling(behandler, true).status shouldBe Behandlingsstatus.UTREDES

        tilbakekreving.håndterForeslåVedtak(behandler)
        tilbakekreving.frontendDtoForBehandling(behandler, true).status shouldBe Behandlingsstatus.FATTER_VEDTAK

        tilbakekreving.håndter(beslutter, godkjenning())
        tilbakekreving.frontendDtoForBehandling(behandler, true).status shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK

        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                vedtakId = BigInteger.ZERO,
            ),
        )
        tilbakekreving.frontendDtoForBehandling(behandler, true).status shouldBe Behandlingsstatus.AVSLUTTET
    }
}
