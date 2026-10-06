package no.nav.tilbakekreving.behandling.revurdering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class RevurderingTest {
    @Test
    fun `Revurdering opprettes riktig for avsluttet sak`() {
        val context = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilAvsluttet(opprettTilbakekrevingHendelse(), context)
        val behandling = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())

        tilbakekreving.tilstand.behandlingsstatus(behandling, context.klokke) shouldBe BehandlingsstatusModell.AVSLUTTET

        tilbakekreving.opprettRevurdering(tilbakekreving.nåværendeBehandlingId(), Behandlingsårsakstype.REVURDERING_KLAGE_KA, saksbehandlerContext())

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), context, true, BehandlerRolle.SAKSBEHANDLER) shouldNotBeNull {
            type shouldBe Behandlingstype.REVURDERING_TILBAKEKREVING
        }
    }

    @Test
    fun `opprettelse av revurdering for ikke avsluttet sak skal feile`() {
        val context = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), context) {
            vurderFakta(faktastegVurdering())
        }
        val behandling = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())

        tilbakekreving.tilstand.behandlingsstatus(behandling, context.klokke) shouldBe BehandlingsstatusModell.TIL_FORHÅNDSVARSEL
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), context, true, BehandlerRolle.SAKSBEHANDLER).kanRevurderingOpprettes shouldBe false
        shouldThrow<IllegalStateException> {
            tilbakekreving.opprettRevurdering(tilbakekreving.nåværendeBehandlingId(), Behandlingsårsakstype.REVURDERING_KLAGE_KA, saksbehandlerContext())
        }.message shouldBe "Behandlingen er i TIL_FORHÅNDSVARSEL. Revurdering kan kun opprette for avsluttet behandling."
    }

    private fun tilbakekrevingTilAvsluttet(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        saksbehandlerContext: SideeffektContext,
    ): Tilbakekreving {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse).apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext) {
                lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Forhåndsvarsel er ikke nødvendig i testen")
                lagreUttalelse(UttalelseVurdering.JA, null, "")
                vurderFakta(faktastegVurdering())
                vurderForeldelse(1.januar(2021) til 31.januar(2021), foreldelseVurdering())
                vurderVilkår(
                    periode = 1.januar(2021) til 31.januar(2021),
                    vurdering = forårsaketAvNav().burdeForstått(aktsomhet = uaktsomt(skalIkkeUnnlates(), ingenReduksjon())),
                )
                foreslåVedtak()
            }
        }
        tilbakekreving.apply {
            gjørSaksbehandling(nåværendeBehandlingId(), beslutterContext()) {
                fatteVedtak(
                    listOf(
                        Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
                    ),
                )
            }
        }
        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                vedtakId = BigInteger.valueOf(1L),
            ),
            saksbehandlerContext,
        )

        val brevId = tilbakekreving.brevHistorikk.sisteVedtaksbrev()!!.id
        tilbakekreving.håndter(
            JournalføringHendelse(
                brevId = brevId,
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                journalpostId = "",
                fagsakId = "",
                dokumentInfoId = "",
            ),
            saksbehandlerContext,
        )

        tilbakekreving.håndter(
            DistribusjonHendelse(
                brevId = brevId,
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                journalpostId = "",
                fagsakId = "",
                dokumentInfoId = "",
            ),
            saksbehandlerContext,
        )

        return tilbakekreving
    }
}
