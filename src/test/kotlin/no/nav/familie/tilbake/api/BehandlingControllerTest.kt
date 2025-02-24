package no.nav.familie.tilbake.api

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDtoTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kontrakter.Fagsystem
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.util.UUID

class BehandlingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        headers.setBearerAuth(lokalTestToken())
        every { ContextService.hentSaksbehandler(any()) } returns "saksbehandler"
    }

    @AfterEach
    fun afterEach() {
        unmockkObject(ContextService)
    }

    @Test
    fun `Man må ha minimumsrolle SAKSBEHANDLER for å bruke endepunkt`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER)))

        val response = flyttBehandlingTilFakta(opprettTestdata())
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Med rollene FORVALTER og SAKSBEHANDLER kan man bruke endepunkt`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER, Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))

        val response = flyttBehandlingTilFakta(opprettTestdata())
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Beslutter som ikke ansvarlig saksbehandler skal ikke kunne bruke forvaltningsendepunkt`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.BESLUTTER)))
        every { ContextService.hentSaksbehandler(any()) } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata("ikkeAnsvarligSaksbehandler"))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Saksbehandler som ansvarlig saksbehandler skal kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))
        every { ContextService.hentSaksbehandler(any()) } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata(saksbehandler = "ansvarligSaksbehandler"))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Saksbehandler som ikke ansvarlig saksbehandler skal få feil`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))
        every { ContextService.hentSaksbehandler(any()) } returns "ansvarligSaksbehandler"
        val behandlingId = opprettTestdata("ikkeAnsvarligSaksbehandler")
        val response = flyttBehandlingTilFakta(behandlingId)
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Behandling må være under utredning for å flyttes tilbake til fakta`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))

        val behandlingId = opprettTestdata(behandlingStatus = Behandlingsstatus.FATTER_VEDTAK)
        val response = flyttBehandlingTilFakta(behandlingId)
        assertThat(HttpStatus.FORBIDDEN).isEqualTo(response.statusCode)
        assertThat(response.body).contains("Behandling er ikke under utredning, og kan derfor ikke flyttes tilbake til fakta")
    }

    @Test
    fun `Skal ikke være mulig å sette behandling på vent tilbake til fakta`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))

        val response = flyttBehandlingTilFakta(opprettTestdata(behandlingsstegsstatus = Behandlingsstegstatus.VENTER))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertThat(response.body).contains("Behandling er på vent og kan derfor ikke flyttes tilbake til fakta")
    }

    @Test
    fun `Saksbehandler uten beslutterrolle kan ikke godkjenne vedtak`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))

        val behandlingId = opprettTestdata(behandlingStatus = Behandlingsstatus.FATTER_VEDTAK)
        val response =
            restTemplate.exchange<Ressurs<Nothing>>(
                localhost("/api/behandling/$behandlingId/steg/v1"),
                HttpMethod.POST,
                HttpEntity(BehandlingsstegFatteVedtaksstegDtoTest.ny(), headers),
            )
        response.statusCode shouldBe HttpStatus.FORBIDDEN
        response.body?.melding shouldBe "saksbehandler med rolle SAKSBEHANDLER har ikke tilgang til å Utfører behandlingens aktiv steg og fortsetter den til neste steg. Krever BESLUTTER."
    }

    @Test
    fun `Saksbehandler med beslutterrolle kan godkjenne vedtak`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.BESLUTTER)))

        val behandlingId = opprettTestdata(behandlingStatus = Behandlingsstatus.FATTER_VEDTAK)
        val response =
            restTemplate.exchange<Ressurs<Nothing>>(
                localhost("/api/behandling/$behandlingId/steg/v1"),
                HttpMethod.POST,
                HttpEntity(BehandlingsstegFatteVedtaksstegDtoTest.ny(), headers),
            )
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.melding shouldBe "ansvarlig beslutter kan ikke være samme som ansvarlig saksbehandler"
    }

    private fun flyttBehandlingTilFakta(
        behandlingId: UUID,
    ): ResponseEntity<String> =
        restTemplate.exchange(
            localhost("/api/behandling/$behandlingId/flytt-behandling-til-fakta"),
            HttpMethod.PUT,
            HttpEntity<String>(headers),
        )

    private fun opprettTestdata(
        saksbehandler: String = "saksbehandler",
        behandlingStatus: Behandlingsstatus = Behandlingsstatus.UTREDES,
        behandlingsstegsstatus: Behandlingsstegstatus = Behandlingsstegstatus.KLAR,
    ): UUID {
        val fagsak =
            Fagsak(
                ytelsestype = Ytelsestype.BARNETRYGD,
                fagsystem = Fagsystem.EF,
                eksternFagsakId = "testverdi",
                bruker = Bruker(ident = "32132132111"),
            )
        val behandling = Testdata.lagBehandling(fagsakId = fagsak.id, ansvarligSaksbehandler = saksbehandler, behandlingStatus = behandlingStatus)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandling.id,
                    behandlingssteg = Behandlingssteg.FAKTA,
                    behandlingsstegsstatus = behandlingsstegsstatus,
                    tidsfrist = LocalDate.now().plusWeeks(3),
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                ),
            )
        return behandling.id
    }
}
