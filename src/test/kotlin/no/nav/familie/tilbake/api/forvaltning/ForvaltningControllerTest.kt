package no.nav.familie.tilbake.api.forvaltning

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
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
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
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
    }

    @AfterEach
    fun afterEach() {
        unmockkObject(ContextService)
    }

    @Test
    fun `beslutter som ansvarlig saksbehandler skal kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.BESLUTTER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata(saksbehandler = "ansvarligSaksbehandler", behandlingsstegsstatus = Behandlingsstegstatus.KLAR))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Ikke mulig å sette behandling på vent tilbake til fakta`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.BESLUTTER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata("ansvarligSaksbehandler"))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `beslutter som ikke ansvarlig saksbehandler skal ikke kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.BESLUTTER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata("ikkeAnsvarligSaksbehandler"))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `saksbehandler som ansvarlig saksbehandler skal kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata(saksbehandler = "ansvarligSaksbehandler", behandlingsstegsstatus = Behandlingsstegstatus.KLAR))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `saksbehandler som ikke ansvarlig saksbehandler skal ikke kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.SAKSBEHANDLER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata("ikkeAnsvarligSaksbehandler"))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `forvalter skal kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.FORVALTER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata(saksbehandler = "ikkeAnsvarligSaksbehandler", behandlingsstegsstatus = Behandlingsstegstatus.KLAR))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `veileder skal ikke kunne sette behandling tilbake til faktasteg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.VEILEDER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata("ansvarligSaksbehandler"))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `skal ikke kunne sette behandling tilbake til fakta når behandling ikke er under utredning`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
            .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.ENSLIG_FORELDER to Behandlerrolle.VEILEDER)))
        every { ContextService.hentSaksbehandler() } returns "ansvarligSaksbehandler"

        val response = flyttBehandlingTilFakta(opprettTestdata("ansvarligSaksbehandler", Behandlingsstatus.FATTER_VEDTAK))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    private fun flyttBehandlingTilFakta(
        behandlingId: UUID,
    ): ResponseEntity<String> {
        return restTemplate.exchange(
            localhost("/api/forvaltning/behandling/$behandlingId/flytt-behandling/v1"),
            HttpMethod.PUT,
            HttpEntity<String>(headers),
        )
    }

    private fun opprettTestdata(
        saksbehandler: String = "saksbehandler",
        behandlingStatus: Behandlingsstatus = Behandlingsstatus.UTREDES,
        behandlingsstegsstatus: Behandlingsstegstatus = Behandlingsstegstatus.VENTER,
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
