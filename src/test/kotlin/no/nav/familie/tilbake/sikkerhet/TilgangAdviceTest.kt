package no.nav.familie.tilbake.sikkerhet

import io.jsonwebtoken.Jwts
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.api.dto.BrukerDto
import no.nav.familie.tilbake.api.dto.FagsakDto
import no.nav.familie.tilbake.api.dto.HentFagsystemsbehandlingRequestDto
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.aspectj.lang.JoinPoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.TestPropertySource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Calendar

@TestPropertySource(properties = ["rolle.barnetrygd.beslutter=bb123",
    "rolle.barnetrygd.saksbehandler=bs123",
    "rolle.barnetrygd.veileder=bv123",
    "rolle.enslig.beslutter=eb123",
    "rolle.enslig.saksbehandler=es123",
    "rolle.enslig.veileder=ev123",
    "rolle.kontantstøtte.beslutter = kb123",
    "rolle.kontantstøtte.saksbehandler = ks123",
    "rolle.kontantstøtte.veileder = kv123",
    "rolle.teamfamilie.forvalter = familie123"])
internal class TilgangAdviceTest : OppslagSpringRunnerTest() {

    companion object {

        const val BARNETRYGD_BESLUTTER_ROLLE = "bb123"
        const val BARNETRYGD_SAKSBEHANDLER_ROLLE = "bs123"
        const val BARNETRYGD_VEILEDER_ROLLE = "bv123"

        const val ENSLIG_BESLUTTER_ROLLE = "eb123"
        const val ENSLIG_SAKSBEHANDLER_ROLLE = "es123"

        const val TEAMFAMILIE_FORVALTER_ROLLE = "familie123"
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.currentRequestAttributes().removeAttribute(SpringTokenValidationContextHolder::class.java.name, 0)
    }


    private lateinit var tilgangAdvice: TilgangAdvice

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    private val mockJoinpoint: JoinPoint = mockk()
    private val mockIntegrasjonerClient: IntegrasjonerClient = mockk()

    @BeforeEach
    fun init() {
        tilgangAdvice = TilgangAdvice(rolleConfig,
                                      behandlingRepository,
                                      fagsakRepository,
                                      mottattXmlRepository,
                                      mockIntegrasjonerClient)

    }

    @Test
    fun `sjekkTilgang skal sperre tilgang hvis person er kode 6`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(false))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)



        shouldThrow<RuntimeException> { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
                .message shouldBe "abc har ikke tilgang til person i hent behandling"
    }

    @Test
    fun `sjekkTilgang skal gi tilgang hvis person ikke er kode 6`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)



        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang for barnetrygd beslutter i barnetrygd hent behandling request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for enslig beslutter i barnetrygd hent behandling request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(ENSLIG_BESLUTTER_ROLLE))
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "barnetrygd hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldThrow<RuntimeException> { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
                .message shouldBe "abc har ikke tilgang til barnetrygd hent behandling"
        val exception = shouldThrow<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       rolletilgangssjekk)
        })
        exception.message shouldBe "abc har ikke tilgang til ${rolletilgangssjekk.handling}"
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for barnetrygd veileder i barnetrygd opprett behandling request`() {
        val token = opprettToken("abc", listOf(BARNETRYGD_VEILEDER_ROLLE))
        opprettRequestContext("/api/behandling/v1", HttpMethod.POST, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(lagOpprettTilbakekrevingRequest())
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "barnetrygd opprett behandling",
                                                    henteParam = HenteParam.INGEN)

        val exception = shouldThrow<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       rolletilgangssjekk)
        })
        exception.message shouldBe "abc med rolle VEILEDER har ikke tilgang til å barnetrygd opprett behandling. " +
                "Krever ${rolletilgangssjekk.minimumBehandlerrolle}."
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i barnetrygd opprett behandling request når bruker både er beslutter og veileder`() {
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE, BARNETRYGD_VEILEDER_ROLLE))
        opprettRequestContext("/api/behandling/v1", HttpMethod.POST, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(emptyList()) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(lagOpprettTilbakekrevingRequest())
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "barnetrygd opprett behandling",
                                                    henteParam = HenteParam.INGEN)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når saksbehandler har tilgang til enslig og barnetrygd`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når bruker er fagsystem`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken(Constants.BRUKER_ID_VEDTAKSLØSNINGEN, listOf())
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang i hent behandling request når bruker er ukjent`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf())
        opprettRequestContext("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                                                    handling = "hent behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        val exception = shouldThrow<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       rolletilgangssjekk)
        })
        exception.message shouldBe "Bruker har mangler tilgang til hent behandling"
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang i Fakta utførBehandlingssteg POST request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        // POST request uten body
        opprettRequestContext("/api/behandling/$behandlingId/steg/v1/", HttpMethod.POST, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId, BehandlingsstegFaktaDto(feilutbetaltePerioder = emptyList(),
                                                                                           begrunnelse = "testverdi"))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Håndterer behandlingens aktiv steg og fortsetter den til neste steg",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ikke ha tilgang i Fattevedtak utførBehandlingssteg POST request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        // POST request uten body
        opprettRequestContext("/api/behandling/$behandlingId/steg/v1/", HttpMethod.POST, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId,
                                                     BehandlingsstegFatteVedtaksstegDto(totrinnsvurderinger = emptyList()))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Håndterer behandlingens aktiv steg og fortsetter den til neste steg",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        val exception = shouldThrow<RuntimeException> { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
        exception.message shouldBe "abc med rolle SAKSBEHANDLER har ikke tilgang til å Håndterer behandlingens aktiv " +
                "steg og fortsetter den til neste steg. Krever BESLUTTER."
    }

    @Test
    fun `sjekkTilgang skal beslutter ha tilgang i Fattevedtak utførBehandlingssteg POST request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        // POST request uten body
        opprettRequestContext("/api/behandling/$behandlingId/steg/v1/", HttpMethod.POST, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandlingId,
                                                     BehandlingsstegFatteVedtaksstegDto(totrinnsvurderinger = emptyList()))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Håndterer behandlingens aktiv steg og fortsetter den til neste steg",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i sett behandling på vent PUT request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("/api/behandling/$behandlingId/vent/v1/", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandling.id,
                                                     BehandlingPåVentDto(venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                                         tidsfrist = LocalDate.now().plusWeeks(2)))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal forvalter ikke ha tilgang til vanlig tjenester`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext("/api/behandling/$behandlingId/vent/v1/", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandling.id,
                                                     BehandlingPåVentDto(venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                                         tidsfrist = LocalDate.now().plusWeeks(2)))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        val exception = shouldThrow<RuntimeException> { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
        exception.message shouldBe "abc med rolle FORVALTER har ikke tilgang til å Setter behandling på vent." +
                " Krever SAKSBEHANDLER."
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ikke ha tilgang til forvaltningstjenester`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("/api/forvaltning//behandling/$behandlingId/tving-henleggelse/v1", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandling.id)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                                                    handling = "Tving henlegger behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        val exception = shouldThrow<RuntimeException> { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
        exception.message shouldBe "abc med rolle SAKSBEHANDLER har ikke tilgang til å kalle forvaltningstjeneste " +
                "Tving henlegger behandling. Krever FORVALTER."
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang til forvaltningstjenester hvis saksbehandler har forvalter rolle også`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext("/api/forvaltning//behandling/$behandlingId/tving-henleggelse/v1", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandling.id)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                                                    handling = "Tving henlegger behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang til vanlig tjenester selv om saksbehandler har forvalter rolle også`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext("/api/behandling/$behandlingId/vent/v1/", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandling.id,
                                                     BehandlingPåVentDto(venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                                         tidsfrist = LocalDate.now().plusWeeks(2)))
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjenester`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))

        // POST request uten body
        opprettRequestContext("/api/forvaltning//behandling/$behandlingId/tving-henleggelse/v1", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(behandling.id)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                                                    handling = "Tving henlegger behandling",
                                                    henteParam = HenteParam.BEHANDLING_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjeneste arkiver mottattXml med input som mottattXmlId`() {
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        val økonomiXmlMottatt = mottattXmlRepository.insert(Testdata.økonomiXmlMottatt)

        // PUT request uten body
        opprettRequestContext("/arkiver/kravgrunnlag/${økonomiXmlMottatt.id}/v1", HttpMethod.PUT, token)

        every { mockJoinpoint.args } returns arrayOf(økonomiXmlMottatt.id)
        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.FORVALTER,
                                                    handling = "Arkiverer mottatt kravgrunnlag",
                                                    henteParam = HenteParam.MOTTATT_XML_ID)

        shouldNotThrowAny { tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på ytelsestype og eksternFagsakId for henteparam`() {
        val fagsak = opprettFagsak(Ytelsestype.BARNETRYGD)
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("dummy", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(Ytelsestype.BARNETRYGD, fagsak.eksternFagsakId)

        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID)

        tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk)

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på dto med ytelsestype og eksternFagsakId`() {
        val fagsak = opprettFagsak(Ytelsestype.BARNETRYGD)
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("dummy", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))

        every { mockJoinpoint.args } returns
                arrayOf(HentFagsystemsbehandlingRequestDto(Ytelsestype.BARNETRYGD, fagsak.eksternFagsakId, ""))

        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.INGEN)

        tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk)

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på fagsystem og eksternFagsakId for henteparam`() {
        val fagsak = opprettFagsak(Ytelsestype.BARNETRYGD)
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("dummy", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))
        every { mockJoinpoint.args } returns arrayOf(Fagsystem.BA, fagsak.eksternFagsakId)

        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID)

        tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk)

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på dto med fagsystem og eksternFagsakId`() {
        val fagsak = opprettFagsak(Ytelsestype.BARNETRYGD)
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("dummy", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))

        every { mockJoinpoint.args } returns
                arrayOf(FagsakDto(fagsak.eksternFagsakId,
                                  Ytelsestype.BARNETRYGD,
                                  Fagsystem.BA,
                                  Språkkode.NB,
                                  BrukerDto("", "", LocalDate.now(), Kjønn.KVINNE),
                                  listOf()))

        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.INGEN)

        tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk)

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på dto med behandlingId`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("dummy", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))

        every { mockJoinpoint.args } returns
                arrayOf(HentForhåndvisningVedtaksbrevPdfDto(behandlingId = behandling.id, perioderMedTekst = listOf()))

        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.INGEN)

        tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk)

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på dto med eksternBrukId`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext("dummy", HttpMethod.PUT, token)

        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) } returns listOf(Tilgang(true))

        every { mockJoinpoint.args } returns
                arrayOf(object {
                    @Suppress("unused") val eksternBrukId = behandling.eksternBrukId
                })

        val rolletilgangssjekk = Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                    handling = "Setter behandling på vent",
                                                    henteParam = HenteParam.INGEN)

        tilgangAdvice.sjekkTilgang(mockJoinpoint, rolletilgangssjekk)

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232")) }
    }

    private fun opprettBehandling(ytelsestype: Ytelsestype): Behandling {
        val fagsak = opprettFagsak(ytelsestype)
        val behandling = Behandling(fagsakId = fagsak.id,
                                    type = Behandlingstype.TILBAKEKREVING,
                                    ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                                    behandlendeEnhet = "8020",
                                    behandlendeEnhetsNavn = "Oslo",
                                    manueltOpprettet = false)
        behandlingRepository.insert(behandling)
        return behandling
    }

    private fun opprettFagsak(ytelsestype: Ytelsestype): Fagsak {
        val fagsak = Fagsak(bruker = Bruker("1232"),
                            eksternFagsakId = "123",
                            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
                            ytelsestype = ytelsestype)
        return fagsakRepository.insert(fagsak)
    }

    private fun opprettToken(behandlerNavn: String, gruppeNavn: List<String>): String {
        val additionalParameters = mapOf("NAVident" to behandlerNavn, "groups" to gruppeNavn)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, 60)
        return Jwts.builder().setExpiration(calendar.time)
                .setIssuer("azuread")
                .addClaims(additionalParameters).compact()

    }

    private fun opprettRequestContext(requestUri: String, requestMethod: HttpMethod, token: String) {
        val mockHttpServletRequest =
                MockHttpServletRequest(requestMethod.name, requestUri)

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockHttpServletRequest))
        val tokenValidationContext = TokenValidationContext(mapOf("azuread" to JwtToken(token)))
        RequestContextHolder.currentRequestAttributes()
                .setAttribute(SpringTokenValidationContextHolder::class.java.name, tokenValidationContext, 0)
    }

    private fun lagOpprettTilbakekrevingRequest(): OpprettTilbakekrevingRequest {
        val varsel = Varsel("hello", BigDecimal.valueOf(1000), emptyList())
        val faktainfo = Faktainfo(revurderingsårsak = "testårsak",
                                  revurderingsresultat = "testresultat",
                                  tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        return OpprettTilbakekrevingRequest(ytelsestype = Ytelsestype.BARNETRYGD,
                                            fagsystem = Fagsystem.BA,
                                            eksternFagsakId = "123",
                                            personIdent = "123434",
                                            eksternId = "123",
                                            manueltOpprettet = false,
                                            enhetId = "8020",
                                            enhetsnavn = "Oslo",
                                            revurderingsvedtaksdato = LocalDate.now(),
                                            varsel = varsel,
                                            faktainfo = faktainfo,
                                            saksbehandlerIdent = "bob")
    }


}
