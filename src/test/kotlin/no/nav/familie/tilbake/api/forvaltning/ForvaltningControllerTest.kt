package no.nav.familie.tilbake.api.forvaltning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
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

class ForvaltningControllerTest : OppslagSpringRunnerTest() {
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
    fun `Forvalter kan sette behandling på vent tilbake til fakta`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.FORVALTER, Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER)))

        val response = flyttBehandlingTilFakta()
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Beslutter skal ikke kunne kalle på forvalterendepunkt`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.BESLUTTER)))
        val response = flyttBehandlingTilFakta()
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Saksbehandler og forvalter som ikke er ansvarlig saksbehandler skal kunne bruke forvaltningsendepunkt`() {
        val tilganger = mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER, Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER)

        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(tilganger))
        every { ContextService.hentSaksbehandler(any()) } returns "ikke ansvarlig"

        val response = flyttBehandlingTilFakta()
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Veileder skal ikke kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.VEILEDER)))

        val response = flyttBehandlingTilFakta()
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Forvalter kan sette behandling tilbake til fakta når behandling ikke er under utredning`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.FORVALTER, Tilgangskontrollsfagsystem.FORVALTER_TILGANG to Behandlerrolle.FORVALTER)))
        val response = flyttBehandlingTilFakta(opprettTestdata(behandlingStatus = Behandlingsstatus.FATTER_VEDTAK))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    private fun flyttBehandlingTilFakta(
        behandlingId: UUID = opprettTestdata(),
    ): ResponseEntity<String> =
        restTemplate.exchange(
            localhost("/api/forvaltning/behandling/$behandlingId/flytt-behandling/v1"),
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
                eksternFagsakId = UUID.randomUUID().toString(),
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
