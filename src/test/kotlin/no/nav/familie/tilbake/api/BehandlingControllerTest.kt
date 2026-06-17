package no.nav.familie.tilbake.api

import io.kotest.matchers.shouldBe
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
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDtoTest
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.assertj.core.api.Assertions.assertThat
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
class BehandlingControllerTest : OppslagSpringRunnerTest() {
    private val restTemplate = TestRestTemplate()

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `Man må ha minimumsrolle SAKSBEHANDLER for å bruke endepunkt`() {
        val response = flyttBehandlingTilFakta(
            opprettTestdata(
                saksbehandler = SAKSBEHANDLER_IDENT,
                behandlingStatus = Behandlingsstatus.UTREDES,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
            ),
            authorizationHeaders(grupper = listOf("forvalter")),
        )
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Med rollene FORVALTER og SAKSBEHANDLER kan man bruke endepunkt`() {
        val response = flyttBehandlingTilFakta(
            behandlingId = opprettTestdata(
                SAKSBEHANDLER_IDENT,
                Behandlingsstatus.UTREDES,
                Behandlingsstegstatus.KLAR,
            ),
            headers = authorizationHeaders(grupper = listOf("forvalter", "ef-saksbehandler")),
        )
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Beslutter som ikke ansvarlig saksbehandler skal ikke kunne bruke forvaltningsendepunkt`() {
        val response = flyttBehandlingTilFakta(
            opprettTestdata(
                saksbehandler = "ikkeAnsvarligSaksbehandler",
                behandlingStatus = Behandlingsstatus.UTREDES,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
            ),
            authorizationHeaders(grupper = listOf("ef-beslutter")),
        )
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Saksbehandler som ansvarlig saksbehandler skal kunne sette behandling tilbake til faktasteg`() {
        val response = flyttBehandlingTilFakta(
            opprettTestdata(
                saksbehandler = SAKSBEHANDLER_IDENT,
                behandlingStatus = Behandlingsstatus.UTREDES,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
            ),
            authorizationHeaders(grupper = listOf("ef-saksbehandler")),
        )
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Saksbehandler som ikke ansvarlig saksbehandler skal få feil`() {
        val response = flyttBehandlingTilFakta(
            opprettTestdata(
                saksbehandler = "ikkeAnsvarligSaksbehandler",
                behandlingStatus = Behandlingsstatus.UTREDES,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
            ),
            authorizationHeaders(grupper = listOf("ef-saksbehandler")),
        )
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Behandling må være under utredning for å flyttes tilbake til fakta`() {
        val response = flyttBehandlingTilFakta(
            opprettTestdata(
                saksbehandler = SAKSBEHANDLER_IDENT,
                behandlingStatus = Behandlingsstatus.FATTER_VEDTAK,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
            ),
            authorizationHeaders(grupper = listOf("ef-saksbehandler")),
        )
        assertThat(HttpStatus.FORBIDDEN).isEqualTo(response.statusCode)
        assertThat(response.body).contains("Behandling er ikke under utredning, og kan derfor ikke flyttes tilbake til fakta")
    }

    @Test
    fun `Skal ikke være mulig å sette behandling på vent tilbake til fakta`() {
        val response = flyttBehandlingTilFakta(
            opprettTestdata(
                saksbehandler = SAKSBEHANDLER_IDENT,
                behandlingStatus = Behandlingsstatus.UTREDES,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
            ),
            authorizationHeaders(grupper = listOf("ef-saksbehandler")),
        )
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertThat(response.body).contains("Behandling er på vent og kan derfor ikke flyttes tilbake til fakta")
    }

    @Test
    fun `Saksbehandler uten beslutterrolle kan ikke godkjenne vedtak`() {
        val behandlingId = opprettTestdata(SAKSBEHANDLER_IDENT, behandlingStatus = Behandlingsstatus.FATTER_VEDTAK, behandlingsstegsstatus = Behandlingsstegstatus.KLAR)
        val response = restTemplate.exchange<Ressurs<Nothing>>(
            localhost("/api/behandling/$behandlingId/steg/v1"),
            HttpMethod.POST,
            HttpEntity(BehandlingsstegFatteVedtaksstegDtoTest.ny(), authorizationHeaders(grupper = listOf("ef-saksbehandler"))),
        )
        response.statusCode shouldBe HttpStatus.FORBIDDEN
        response.body?.melding shouldBe "$SAKSBEHANDLER_IDENT med rolle SAKSBEHANDLER har ikke tilgang til å Utfører behandlingens aktiv steg og fortsetter den til neste steg. Krever BESLUTTER."
    }

    @Test
    fun `Saksbehandler med beslutterrolle kan godkjenne vedtak`() {
        val behandlingId = opprettTestdata(
            saksbehandler = SAKSBEHANDLER_IDENT,
            behandlingStatus = Behandlingsstatus.FATTER_VEDTAK,
            behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
        )
        val response = restTemplate.exchange<Ressurs<Nothing>>(
            localhost("/api/behandling/$behandlingId/steg/v1"),
            HttpMethod.POST,
            HttpEntity(BehandlingsstegFatteVedtaksstegDtoTest.ny(), authorizationHeaders(grupper = listOf("ef-beslutter"))),
        )
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.melding shouldBe "ansvarlig beslutter kan ikke være samme som ansvarlig saksbehandler"
    }

    private fun flyttBehandlingTilFakta(
        behandlingId: UUID,
        headers: HttpHeaders,
    ): ResponseEntity<String> = restTemplate.exchange(
        localhost("/api/behandling/$behandlingId/flytt-behandling-til-fakta"),
        HttpMethod.PUT,
        HttpEntity<String>(headers),
    )

    private fun opprettTestdata(
        saksbehandler: String,
        behandlingStatus: Behandlingsstatus,
        behandlingsstegsstatus: Behandlingsstegstatus,
    ): UUID {
        val fagsak = Fagsak(
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.EF,
            eksternFagsakId = UUID.randomUUID().toString(),
            bruker = Bruker(ident = "32132132111"),
        )
        val behandling = Testdata.lagBehandling(fagsakId = fagsak.id, ansvarligSaksbehandler = saksbehandler, behandlingStatus = behandlingStatus)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        behandlingsstegstilstandRepository.insert(
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
