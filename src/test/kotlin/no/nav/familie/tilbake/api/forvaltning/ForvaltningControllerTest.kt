package no.nav.familie.tilbake.api.forvaltning

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.util.UUID

@TestPropertySource(
    properties = [
        "rolle.enslig.veileder=ef-veileder",
        "rolle.enslig.saksbehandler=ef-saksbehandler",
        "rolle.enslig.beslutter=ef-beslutter",
        "rolle.teamfamilie.forvalter=forvalter",
    ],
)
class ForvaltningControllerTest : OppslagSpringRunnerTest() {
    private val restTemplate = TestRestTemplate()

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Test
    fun `Forvalter kan sette behandling på vent tilbake til fakta`() {
        val headers = authorizationHeaders(grupper = listOf("forvalter"))

        val response = flyttBehandlingTilFakta(opprettTestdata(), headers)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Beslutter skal ikke kunne kalle på forvalterendepunkt`() {
        val headers = authorizationHeaders(grupper = listOf("ef-beslutter"))
        val response = flyttBehandlingTilFakta(opprettTestdata(), headers)
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Saksbehandler og forvalter som ikke er ansvarlig saksbehandler skal kunne bruke forvaltningsendepunkt`() {
        val headers = authorizationHeaders(ident = "ikke ansvarlig", grupper = listOf("forvalter", "ef-saksbehandler"))

        val response = flyttBehandlingTilFakta(opprettTestdata(), headers)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Veileder skal ikke kunne sette behandling tilbake til faktasteg`() {
        val headers = authorizationHeaders(grupper = listOf("ef-veileder"))

        val response = flyttBehandlingTilFakta(opprettTestdata(), headers)
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Forvalter kan sette behandling tilbake til fakta når behandling ikke er under utredning`() {
        val headers = authorizationHeaders(grupper = listOf("forvalter"))
        val response = flyttBehandlingTilFakta(opprettTestdata(behandlingStatus = Behandlingsstatus.FATTER_VEDTAK), headers)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    private fun flyttBehandlingTilFakta(
        behandlingId: UUID,
        headers: HttpHeaders,
    ): ResponseEntity<String> =
        restTemplate.exchange(
            localhost("/api/forvaltning/behandling/$behandlingId/flytt-behandling/v1"),
            HttpMethod.PUT,
            HttpEntity<String>(headers),
        )

    private fun opprettTestdata(behandlingStatus: Behandlingsstatus = Behandlingsstatus.UTREDES): UUID {
        val fagsak = Fagsak(
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.EF,
            eksternFagsakId = UUID.randomUUID().toString(),
            bruker = Bruker(ident = "32132132111"),
        )
        val behandling = Testdata.lagBehandling(fagsakId = fagsak.id, ansvarligSaksbehandler = SAKSBEHANDLER_IDENT, behandlingStatus = behandlingStatus)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.FAKTA,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
                tidsfrist = LocalDate.now().plusWeeks(3),
                venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            ),
        )
        return behandling.id
    }
}
