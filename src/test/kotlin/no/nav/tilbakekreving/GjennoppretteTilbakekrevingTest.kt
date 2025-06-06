package no.nav.tilbakekreving

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.PeriodeMedTekstDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertTotrinnDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.AvventerFagsysteminfo
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class GjennoppretteTilbakekrevingTest : OppslagSpringRunnerTest() {
    val behandler = Behandler.Saksbehandler("Z999999")

    fun lagTilbakekrevingKlarTilBehandling(): Tilbakekreving {
        val opprettEvent = opprettTilbakekrevingEvent(opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent)
        tilbakekrevingService.håndterHendleseForTest(opprettEvent, tilbakekreving)
        tilbakekrevingService.håndterHendleseForTest(kravgrunnlag(), tilbakekreving)
        tilbakekrevingService.håndterHendleseForTest(fagsysteminfoHendelse(), tilbakekreving)
        tilbakekrevingService.håndterHendleseForTest(brukerinfoHendelse(), tilbakekreving)
        tilbakekrevingService.håndterHendleseForTest(VarselbrevSendtHendelse(varselbrev()), tilbakekreving)
        return tilbakekreving
    }

    @Autowired
    lateinit var tilbakekrevingService: TilbakekrevingService

    @Test
    fun `gjennopprette tilbakekreving etter at hendelsene er håndtert`() {
        val opprettEvent = opprettTilbakekrevingEvent(opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(BehovObservatørOppsamler(), opprettEvent)

        tilbakekreving.hentTilstandsnavn() shouldBe Start.navn
        var gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving shouldBe null

        tilbakekrevingService.håndterHendleseForTest(opprettEvent, tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        tilbakekreving.hentTilstandsnavn() shouldBe AvventerKravgrunnlag.navn
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe tilbakekreving.hentTilstandsnavn()

        tilbakekrevingService.håndterHendleseForTest(kravgrunnlag(), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        tilbakekreving.hentTilstandsnavn() shouldBe AvventerFagsysteminfo.navn
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe tilbakekreving.hentTilstandsnavn()

        tilbakekrevingService.håndterHendleseForTest(fagsysteminfoHendelse(), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        tilbakekreving.hentTilstandsnavn() shouldBe AvventerBrukerinfo.navn
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe tilbakekreving.hentTilstandsnavn()

        tilbakekrevingService.håndterHendleseForTest(brukerinfoHendelse(), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        tilbakekreving.hentTilstandsnavn() shouldBe SendVarselbrev.navn
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe tilbakekreving.hentTilstandsnavn()

        tilbakekrevingService.håndterHendleseForTest(VarselbrevSendtHendelse(varselbrev()), tilbakekreving)
        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        tilbakekreving.hentTilstandsnavn() shouldBe TilBehandling.navn
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe tilbakekreving.hentTilstandsnavn()
    }

    @Test
    fun `gjennopprette tilbakekreving etter håndtering av foreldelse`() {
        val tilbakekreving = lagTilbakekrevingKlarTilBehandling()
        val logContext = SecureLog.Context.fra(tilbakekreving)

        val behandlingsstegForeldelseDto =
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        Datoperiode(
                            LocalDate.of(2010, 1, 1),
                            LocalDate.of(2010, 1, 31),
                        ),
                        "foreldelses begrunnelse",
                        Foreldelsesvurderingstype.FORELDET,
                    ),
                    ForeldelsesperiodeDto(
                        Datoperiode(
                            LocalDate.of(2024, 1, 1),
                            LocalDate.of(2024, 1, 31),
                        ),
                        "ikke foreldet. ikke eldre enn 3 år",
                        Foreldelsesvurderingstype.IKKE_FORELDET,
                    ),
                ),
            )

        var gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe "TilBehandling"
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.foreldelsesteg.erFullstending() shouldBe false

        tilbakekrevingService.utførSteg(
            behandler = behandler,
            tilbakekreving = tilbakekreving,
            behandlingsstegDto = behandlingsstegForeldelseDto,
            logContext = logContext,
        )

        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.foreldelsesteg.erFullstending() shouldBe true
    }

    @Test
    fun `gjennopprette tilbakekreving etter vilkårsvurdering`() {
        val tilbakekreving = lagTilbakekrevingKlarTilBehandling()
        val logContext = SecureLog.Context.fra(tilbakekreving)

        val behandlingsstegVilkårsvurderingDto =
            BehandlingsstegVilkårsvurderingDto(
                listOf(
                    VilkårsvurderingsperiodeDto(
                        Datoperiode(
                            LocalDate.of(2024, 1, 1),
                            LocalDate.of(2024, 1, 31),
                        ),
                        Vilkårsvurderingsresultat.GOD_TRO,
                        begrunnelse = "Begrunnelse",
                        godTroDto = GodTroDto(
                            beløpErIBehold = false,
                            begrunnelse = "Beløpt ikke i behold",
                        ),
                    ),
                    VilkårsvurderingsperiodeDto(
                        Datoperiode(
                            LocalDate.of(2024, 10, 1),
                            LocalDate.of(2024, 10, 31),
                        ),
                        Vilkårsvurderingsresultat.GOD_TRO,
                        begrunnelse = "Begrunnelse",
                        godTroDto = GodTroDto(
                            beløpErIBehold = true,
                            begrunnelse = "Beløpt ikke i behold",
                            beløpTilbakekreves = 1500.0.toBigDecimal(),
                        ),
                    ),
                ),
            )

        var gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe "TilBehandling"
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.vilkårsvurderingsteg.erFullstending() shouldBe false

        tilbakekrevingService.utførSteg(
            behandler = behandler,
            tilbakekreving = tilbakekreving,
            behandlingsstegDto = behandlingsstegVilkårsvurderingDto,
            logContext = logContext,
        )

        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.vilkårsvurderingsteg.erFullstending() shouldBe true
    }

    @Test
    fun `gjennopprette tilbakekreving etter behandling av foreslåtVedtakssteg`() {
        val tilbakekreving = lagTilbakekrevingKlarTilBehandling()
        val logContext = SecureLog.Context.fra(tilbakekreving)

        val behandlingsstegForeslåVedtaksstegDto =
            BehandlingsstegForeslåVedtaksstegDto(
                FritekstavsnittDto(
                    oppsummeringstekst = "Dette er oppsummeringstekst",
                    listOf(
                        PeriodeMedTekstDto(
                            Datoperiode(
                                LocalDate.of(2024, 1, 1),
                                LocalDate.of(2024, 1, 31),
                            ),
                        ),
                    ),
                ),
            )

        var gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe "TilBehandling"
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.foreslåVedtakSteg.erFullstending() shouldBe false

        tilbakekrevingService.utførSteg(
            behandler = behandler,
            tilbakekreving = tilbakekreving,
            behandlingsstegDto = behandlingsstegForeslåVedtaksstegDto,
            logContext = logContext,
        )
        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.foreslåVedtakSteg.erFullstending() shouldBe true
    }

    @Test
    fun `gjennopprette tilbakekreving etter behandling av fatte vedtaksteg`() {
        val tilbakekreving = lagTilbakekrevingKlarTilBehandling()
        val logContext = SecureLog.Context.fra(tilbakekreving)

        val behandlingsstegFatteVedtaksstegDto =
            BehandlingsstegFatteVedtaksstegDto(
                listOf(
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.FAKTA,
                        godkjent = true,
                        begrunnelse = "fakta totrinn begrunnelse",
                    ),
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.FORELDELSE,
                        godkjent = true,
                        begrunnelse = "foreldelse totrinn begrunnelse",
                    ),
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                        godkjent = true,
                        begrunnelse = "vilkårsvurdering totrinn begrunnelse",
                    ),
                    VurdertTotrinnDto(
                        behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                        godkjent = true,
                        begrunnelse = "foreslåvedtak totrinn begrunnelse",
                    ),
                ),
            )

        var gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.hentTilstandsnavn() shouldBe "TilBehandling"
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.fatteVedtakSteg.erFullstending() shouldBe false

        tilbakekrevingService.utførSteg(
            behandler = behandler,
            tilbakekreving = tilbakekreving,
            behandlingsstegDto = behandlingsstegFatteVedtaksstegDto,
            logContext = logContext,
        )

        gjennopprettetTilbakekreving = tilbakekrevingService.gjennopprettTilbakekreving(tilbakekreving.id)
        gjennopprettetTilbakekreving!!.behandlingHistorikk.nåværende().entry.fatteVedtakSteg.erFullstending() shouldBe true
    }

    @Test
    fun `gjennopprette tilbakekreving etter behandling av faktasteg`() {
        // ToDo
    }
}
