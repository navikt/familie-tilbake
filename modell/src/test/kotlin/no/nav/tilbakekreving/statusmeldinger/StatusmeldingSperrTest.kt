package no.nav.tilbakekreving.statusmeldinger

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.e2e.kravgrunnlagMedBeløp
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtak
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
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
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.Sperret
import no.nav.tilbakekreving.tilstand.TilBehandling
import no.nav.tilbakekreving.vurderAlleSteg
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class StatusmeldingSperrTest {
    @Test
    fun `sperrStatusmelding feiler når tilstanden er før AvventerBrukerinfo`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context).apply {
            håndter(kravgrunnlag(), context)
        }
        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                sperrStatusmelding(),
                systemContext(),
            )
        }
    }

    @Test
    fun `sperrStatusmelding feiler etter at behandlingen er iverksatt`() {
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
        tilbakekreving.apply {
            håndter(
                IverksettelseHendelse(
                    iverksattVedtakId = UUID.randomUUID(),
                    behandlingId = UUID.randomUUID(),
                    vedtakId = 0.toBigInteger(),
                ),
                saksbehandlerContext,
            )
        }

        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.håndter(
                sperrStatusmelding(),
                systemContext(),
            )
        }
    }

    @Test
    fun `sperrStatusmelding blokkerer behandlingen når den er i AvventerBrukerinfo`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context)

        tilbakekreving.apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }

        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        tilbakekreving.håndter(
            sperrStatusmelding(),
            systemContext(),
        )

        tilbakekreving.apply {
            håndter(brukerinfoHendelse(), context)
        }

        tilbakekreving.tilstand shouldBe Sperret
    }

    @Test
    fun `sperrStatusmelding blokkerer behandlingen når den er i TilBehandling`() {
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
    fun `sperrStatusmelding blokkerer behandlingen når den er i IverksettVedtak tilstand`() {
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
            venter shouldNotBeNull {
                grunn shouldBe Venteårsak.SPERRET_VENTER_PÅ_OPPDATERING_FRA_ØKONOMI
                frist shouldBe LocalDate.now().plusDays(1)
            }
        }
    }

    @Test
    fun `ny statusmelding sender behandlingen til TilBehandling etter at den ble sperret i AvventerBrukerinfo`() {
        val context = saksbehandlerContext()
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context)

        tilbakekreving.apply {
            håndter(kravgrunnlag(), context)
            håndter(fagsysteminfoHendelse(), context)
        }

        tilbakekreving.tilstand shouldBe AvventerBrukerinfo
        tilbakekreving.håndter(
            sperrStatusmelding(),
            systemContext(),
        )

        tilbakekreving.apply {
            håndter(brukerinfoHendelse(), context)
        }

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
            venter shouldBe null
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
            venter shouldBe null
        }
    }
}
