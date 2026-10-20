package no.nav.tilbakekreving.statusmeldinger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.e2e.kravgrunnlagMedBeløp
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtak
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.Kravstatuskode
import no.nav.tilbakekreving.hendelse.StatusmeldingHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekreving
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.sperrStatusmelding
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import no.nav.tilbakekreving.tilstand.Sperret
import no.nav.tilbakekreving.tilstand.TilBehandling
import no.nav.tilbakekreving.vurderAlleSteg
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class StatusmeldingSperrTest {
    @Test
    fun `sperrStatusmelding feiler når tilstanden er før til_behandling`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context).apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                sperrStatusmelding(),
                systemContext(),
            )
        }
    }

    @Test
    fun `sperrStatusmelding feiler etter behandling er iverksatt`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context).apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                sperrStatusmelding(),
                systemContext(),
            )
        }
    }

    @Test
    fun `sperrStatusmelding blokkerer behandling når den er i til_behandling`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse).apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext()) {
                vurderFakta(faktastegVurdering())
            }
        }
        tilbakekreving.håndter(
            sperrStatusmelding(),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe Sperret
    }

    @Test
    fun `sperrStatusmelding blokkerer behandling når den er i IverksettVedtak tilstand`() {
        val periode = 1.januar(2025) til 31.januar(2025)
        val saksbehandlerContext = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilBehandling(kravgrunnlag = kravgrunnlagMedBeløp(periode to 2000.kroner))
        tilbakekreving.vurderAlleSteg(saksbehandlerContext, periode)
        tilbakekreving.apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext) {
                foreslåVedtak()
            }
        }
        tilbakekreving.fatteVedtak()

        tilbakekreving.håndter(
            sperrStatusmelding(),
            systemContext(),
        )
        tilbakekreving.tilstand shouldBe Sperret
        tilbakekreving.frontendDtoForBehandling(
            tilbakekreving.nåværendeBehandlingId(),
            sideeffektContext = saksbehandlerContext,
            kanBeslutte = false,
            behandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        ).shouldNotBeNull {
            kanEndres shouldBe false
            erBehandlingPåVent shouldBe true
            behandlingsstegsinfo.forEach {
                it.venteårsak shouldBe Venteårsak.SPERRET_VENTER_PÅ_OPPDATERING_FRA_ØKONOMI
            }
        }
    }

    @Test
    fun `sperret behandling oppheves etter ny statusmelding`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse).apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext()) {
                vurderFakta(faktastegVurdering())
            }
        }
        tilbakekreving.håndter(
            sperrStatusmelding(),
            systemContext(),
        )

        tilbakekreving.håndter(
            StatusmeldingHendelse(
                id = UUID.randomUUID(),
                opprettet = LocalDateTime.now(),
                vedtakId = 1234.toBigInteger(),
                kravstatuskode = Kravstatuskode.ENDRET,
                vedtakGjelder = Aktør.Person("12345678901"),
                referanse = "0",
                kodeFagomraade = "BA",
                fagsystemId = "123",
            ),
            systemContext(),
        )

        tilbakekreving.tilstand shouldBe TilBehandling
        tilbakekreving.frontendDtoForBehandling(
            tilbakekreving.nåværendeBehandlingId(),
            sideeffektContext = saksbehandlerContext(),
            kanBeslutte = false,
            behandlerRolle = BehandlerRolle.SAKSBEHANDLER,
        ).shouldNotBeNull {
            kanEndres shouldBe true
            erBehandlingPåVent shouldBe false
            behandlingsstegsinfo.forEach {
                it.venteårsak shouldBe null
            }
        }
    }
}
