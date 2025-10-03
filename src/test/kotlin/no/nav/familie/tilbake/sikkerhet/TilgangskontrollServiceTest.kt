package no.nav.familie.tilbake.sikkerhet

import io.jsonwebtoken.Jwts
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.tilgangskontroll.Tilgang
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.TestPropertySource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.Calendar
import java.util.UUID

@TestPropertySource(
    properties = [
        "rolle.barnetrygd.beslutter=bb123",
        "rolle.barnetrygd.saksbehandler=bs123",
        "rolle.barnetrygd.veileder=bv123",
        "rolle.enslig.beslutter=eb123",
        "rolle.enslig.saksbehandler=es123",
        "rolle.enslig.veileder=ev123",
        "rolle.kontantstøtte.beslutter = kb123",
        "rolle.kontantstøtte.saksbehandler = ks123",
        "rolle.kontantstøtte.veileder = kv123",
        "rolle.teamfamilie.forvalter = familie123",
        "tilbakekreving.tilgangsstyring.grupper.ts.saksbehandler = ts-saksbehandler",
        "tilbakekreving.tilgangsstyring.grupper.ts.beslutter = ts-beslutter",
    ],
)
internal class TilgangskontrollServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    companion object {
        const val BARNETRYGD_BESLUTTER_ROLLE = "bb123"
        const val BARNETRYGD_SAKSBEHANDLER_ROLLE = "bs123"
        const val BARNETRYGD_VEILEDER_ROLLE = "bv123"

        const val ENSLIG_BESLUTTER_ROLLE = "eb123"
        const val ENSLIG_SAKSBEHANDLER_ROLLE = "es123"
        const val ENSLIG_VEILEDER_ROLLE = "ev123"

        const val TILLEGSSTØNADER_BESLUTTER_ROLLE = "ts-beslutter"
        const val TILLEGSSTØNADER_SAKSBEHANDLER_ROLLE = "ts-saksbehandler"

        const val TEAMFAMILIE_FORVALTER_ROLLE = "familie123"
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.currentRequestAttributes().removeAttribute(SpringTokenValidationContextHolder::class.java.name, 0)
    }

    private lateinit var tilgangskontrollService: TilgangskontrollService

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var persontilgangService: PersontilgangService

    private val bigQueryService = BigQueryServiceStub()

    private val auditLogger: AuditLogger = mockk(relaxed = true)
    private val personIdent: String = "1232"
    private val mockIntegrasjonerClient: IntegrasjonerClient = mockk()

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        tilgangskontrollService = TokenSupportTilgangskontrollService(
            applicationProperties,
            fagsakRepository,
            behandlingRepository,
            kravgrunnlagRepository,
            auditLogger,
            mottattXmlRepository,
            mockIntegrasjonerClient,
            persontilgangService,
            mockk(relaxed = true),
        )

        fagsak = fagsakRepository.insert(
            Testdata.fagsak().copy(
                bruker = Bruker("1232"),
                fagsystem = Fagsystem.BA,
                ytelsestype = Ytelsestype.BARNETRYGD,
            ),
        )
        behandling = behandlingRepository.insert(
            Behandling(
                fagsakId = fagsak.id,
                type = Behandlingstype.TILBAKEKREVING,
                ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                behandlendeEnhet = "8020",
                behandlendeEnhetsNavn = "Oslo",
                manueltOpprettet = false,
                begrunnelseForTilbakekreving = null,
            ),
        )
    }

    @Test
    fun `sjekkTilgang skal sperre tilgang hvis person er kode 6`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, false, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
        exception.message shouldBe "abc har ikke tilgang til person i test"
    }

    @Test
    fun `sjekkTilgang skal ha tilgang for barnetrygd beslutter i barnetrygd hent behandling request`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for enslig beslutter i barnetrygd hent behandling request`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true))
        val token = opprettToken("abc", listOf(ENSLIG_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        val exception =
            shouldThrow<RuntimeException> {
                tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test")
            }
        exception.message shouldBe "abc har ikke tilgang til test"
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for barnetrygd veileder i barnetrygd opprett behandling request`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true))
        val token = opprettToken("abc", listOf(BARNETRYGD_VEILEDER_ROLLE))
        opprettRequestContext(token)

        val exception =
            shouldThrow<RuntimeException> {
                tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test")
            }
        exception.message shouldBe "abc med rolle VEILEDER har ikke tilgang til å test. Krever ${Behandlerrolle.SAKSBEHANDLER}."
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i barnetrygd opprett behandling request når bruker både er beslutter og veileder`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(emptyList(), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE, BARNETRYGD_VEILEDER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når saksbehandler har tilgang til enslig og barnetrygd`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når bruker er fagsystem`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken(Constants.BRUKER_ID_VEDTAKSLØSNINGEN, listOf())
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang i hent behandling request når bruker er ukjent`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf())
        opprettRequestContext(token)

        val exception =
            shouldThrow<RuntimeException> {
                tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test")
            }
        exception.message shouldBe "Bruker har mangler tilgang til test"
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang i Fakta utførBehandlingssteg POST request`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal beslutter ha tilgang i Fattevedtak utførBehandlingssteg POST request`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ikke ha tilgang til vanlig tjenester`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
        exception.message shouldBe "abc med rolle FORVALTER har ikke tilgang til å test. Krever ${Behandlerrolle.SAKSBEHANDLER}."
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ikke ha tilgang til forvaltningstjenester`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
        exception.message shouldBe "abc uten rolle ${Behandlerrolle.FORVALTER} har ikke tilgang til å kalle forvaltningstjeneste test."
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang til forvaltningstjenester hvis saksbehandler har saksbehandler rolle også`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang til vanlig tjenester selv om saksbehandler har forvalter rolle også`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjenester`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjeneste arkiver mottattXml med input som mottattXmlId`() {
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        val økonomiXmlMottatt = mottattXmlRepository.insert(Testdata.getøkonomiXmlMottatt())
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangMottattXMLId(økonomiXmlMottatt.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjeneste annuler kravgrunnlag med input som eksternKravgrunnlagId`() {
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        val økonomiXmlMottatt = mottattXmlRepository.insert(Testdata.getøkonomiXmlMottatt())
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangKravgrunnlagId(økonomiXmlMottatt.eksternKravgrunnlagId!!, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på ytelsestype og eksternFagsakId for henteparam`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(Ytelsestype.BARNETRYGD, fagsak.eksternFagsakId, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test")

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på fagsystem og eksternFagsakId for henteparam`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(FagsystemDTO.BA, fagsak.eksternFagsakId, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test")

        verify { mockIntegrasjonerClient.sjekkTilgangTilPersoner(listOf("1232"), any()) }
    }

    @Test
    fun `validerTilgangTilbakekreving gir tilgang til behandler med kode 6 og 7`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Barnetrygd), behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `validerTilgangTilbakekreving gir ikke tilgang til behandler uten forvaltningstilgang`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(Tilgang(personIdent, false, null))
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldThrow<ForbiddenError> { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Barnetrygd), behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `forvalterrolle sammen med veileder burde ikke gi tilgang til hva som helst`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(ENSLIG_VEILEDER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldThrow<ForbiddenError> { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Barnetrygd), behandling.id, Behandlerrolle.BESLUTTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `tilleggsstønader - krever beslutter rolle, men har saksbehandler`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(TILLEGSSTØNADER_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldThrow<ForbiddenError> { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Tilleggsstønad), behandling.id, Behandlerrolle.BESLUTTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `tilleggsstønader - krever saksbehandler rolle, har tilgang`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(TILLEGSSTØNADER_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Tilleggsstønad), behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `tilleggsstønader - krever beslutter rolle, har tilgang`() {
        every { mockIntegrasjonerClient.sjekkTilgangTilPersoner(any(), any()) } returns listOf(Tilgang(personIdent, true, null))
        val token = opprettToken("abc", listOf(TILLEGSSTØNADER_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Tilleggsstønad), behandling.id, Behandlerrolle.BESLUTTER, AuditLoggerEvent.ACCESS, "test") }
    }

    private fun opprettToken(
        behandlerNavn: String,
        gruppeNavn: List<String>,
    ): String {
        val additionalParameters = mapOf("NAVident" to behandlerNavn, "groups" to gruppeNavn, "roles" to emptySet<String>())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, 60)
        return Jwts
            .builder()
            .setExpiration(calendar.time)
            .setIssuer("azuread")
            .addClaims(additionalParameters)
            .compact()
    }

    private fun opprettRequestContext(token: String) {
        val mockHttpServletRequest = MockHttpServletRequest(HttpMethod.POST.name(), "test")

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockHttpServletRequest))
        val tokenValidationContext = TokenValidationContext(mapOf("azuread" to JwtToken(token)))
        RequestContextHolder
            .currentRequestAttributes()
            .setAttribute(SpringTokenValidationContextHolder::class.java.name, tokenValidationContext, 0)
    }

    private fun tilbakekreving(ytelse: Ytelse): Tilbakekreving {
        return Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = OpprettTilbakekrevingHendelse(
                opprettelsesvalg = Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
                eksternFagsak = OpprettTilbakekrevingHendelse.EksternFagsak(
                    "1abc",
                    ytelse,
                ),
            ),
            behovObservatør = mockk(relaxed = true),
            bigQueryService = bigQueryService,
            endringObservatør = EndringObservatørOppsamler(),
        )
    }
}
