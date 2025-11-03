package no.nav.familie.tilbake.sikkerhet

import io.jsonwebtoken.Jwts
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.tilbakekreving.integrasjoner.persontilgang.Persontilgang
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
import java.util.EnumMap
import java.util.Optional
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
        "tilbakekreving.toggles.tilgangsmaskinen-enabled = true",
    ],
)
internal class NyTilgangskontrollServiceTest : OppslagSpringRunnerTest() {
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

        private val personIdent = "1232"

        val fagsak = Testdata.fagsak().copy(
            bruker = Bruker(ident = personIdent),
            fagsystem = Fagsystem.BA,
            ytelsestype = Ytelsestype.BARNETRYGD,
        )
        private val behandling = Behandling(
            fagsakId = fagsak.id,
            type = Behandlingstype.TILBAKEKREVING,
            ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
            behandlendeEnhet = "8020",
            behandlendeEnhetsNavn = "Oslo",
            manueltOpprettet = false,
            begrunnelseForTilbakekreving = null,
        )
    }

    @AfterEach
    fun tearDown() {
        RequestContextHolder.currentRequestAttributes().removeAttribute(SpringTokenValidationContextHolder::class.java.name, 0)
    }

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val mottattXmlRepository = mockk<ØkonomiXmlMottattRepository>()
    private val persontilgangService = mockk<PersontilgangService>()
    private lateinit var tilgangskontrollService: TilgangskontrollService

    private val bigQueryService = BigQueryServiceStub()

    @BeforeEach
    fun setup() {
        tilgangskontrollService = TokenSupportTilgangskontrollService(
            applicationProperties = applicationProperties,
            fagsakRepository = mockk {
                every { findByFagsystemAndEksternFagsakId(any(), fagsak.eksternFagsakId) } returns fagsak
                every { findById(any()) } returns Optional.of(fagsak)
            },
            behandlingRepository = mockk {
                every { findById(any()) } returns Optional.of(behandling)
            },
            kravgrunnlagRepository = mockk {
                every { findByEksternKravgrunnlagIdAndAktivIsTrue(any()) } returns null
            },
            auditLogger = mockk(relaxed = true),
            økonomiXmlMottattRepository = mottattXmlRepository,
            integrasjonerClient = mockk(),
            persontilgangService = persontilgangService,
            tokenValidationContextHolder = mockk(relaxed = true),
        )
    }

    @Test
    fun `sjekkTilgang skal sperre tilgang hvis person er kode 6`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.IkkeTilgang("Kode 6", Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_STRENGT_FORTROLIG_ADRESSE)
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
        exception.message shouldBe "abc har ikke tilgang til person i test"
    }

    @Test
    fun `sjekkTilgang skal ha tilgang for barnetrygd beslutter i barnetrygd hent behandling request`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for enslig beslutter i barnetrygd hent behandling request`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(ENSLIG_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> {
            tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test")
        }
        exception.message shouldBe "abc har ikke tilgang til test"
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for barnetrygd veileder i barnetrygd opprett behandling request`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_VEILEDER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> {
            tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test")
        }
        exception.message shouldBe "abc med rolle VEILEDER har ikke tilgang til å test. Krever ${Behandlerrolle.SAKSBEHANDLER}."
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i barnetrygd opprett behandling request når bruker både er beslutter og veileder`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE, BARNETRYGD_VEILEDER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når saksbehandler har tilgang til enslig og barnetrygd`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når bruker er fagsystem`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken(Constants.BRUKER_ID_VEDTAKSLØSNINGEN, listOf())
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang i hent behandling request når bruker er ukjent`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf())
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> {
            tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.VEILEDER, AuditLoggerEvent.ACCESS, "test")
        }
        exception.message shouldBe "Bruker har mangler tilgang til test"
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang i Fakta utførBehandlingssteg POST request`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal beslutter ha tilgang i Fattevedtak utførBehandlingssteg POST request`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ikke ha tilgang til vanlig tjenester`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
        exception.message shouldBe "abc med rolle FORVALTER har ikke tilgang til å test. Krever ${Behandlerrolle.SAKSBEHANDLER}."
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ikke ha tilgang til forvaltningstjenester`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        val exception = shouldThrow<RuntimeException> { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
        exception.message shouldBe "abc uten rolle ${Behandlerrolle.FORVALTER} har ikke tilgang til å kalle forvaltningstjeneste test."
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang til forvaltningstjenester hvis saksbehandler har saksbehandler rolle også`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal saksbehandler ha tilgang til vanlig tjenester selv om saksbehandler har forvalter rolle også`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjenester`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangBehandlingID(behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjeneste arkiver mottattXml med input som mottattXmlId`() {
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)
        val økonomiXmlMottatt = Testdata.getøkonomiXmlMottatt()
        every { mottattXmlRepository.findById(any()) } returns Optional.of(økonomiXmlMottatt)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangMottattXMLId(økonomiXmlMottatt.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal forvalter ha tilgang til forvaltningstjeneste annuler kravgrunnlag med input som eksternKravgrunnlagId`() {
        val token = opprettToken("abc", listOf(TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)
        val økonomiXmlMottatt = Testdata.getøkonomiXmlMottatt()
        every { mottattXmlRepository.findByEksternKravgrunnlagId(any()) } returns økonomiXmlMottatt

        shouldNotThrowAny { tilgangskontrollService.validerTilgangKravgrunnlagId(økonomiXmlMottatt.eksternKravgrunnlagId!!, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på ytelsestype og eksternFagsakId for henteparam`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        tilgangskontrollService.validerTilgangYtelsetypeOgFagsakId(Ytelsestype.BARNETRYGD, fagsak.eksternFagsakId, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test")

        coVerify { persontilgangService.sjekkPersontilgang(any(), "1232") }
    }

    @Test
    fun `sjekkTilgang skal finne personer basert på fagsystem og eksternFagsakId for henteparam`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), "1232") } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(FagsystemDTO.BA, fagsak.eksternFagsakId, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test")

        coVerify { persontilgangService.sjekkPersontilgang(any(), "1232") }
    }

    @Test
    fun `validerTilgangTilbakekreving gir tilgang til behandler med kode 6 og 7`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Barnetrygd), behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `validerTilgangTilbakekreving gir ikke tilgang til behandler uten forvaltningstilgang`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.IkkeTilgang("Kode 6", Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_STRENGT_FORTROLIG_ADRESSE)
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldThrow<ForbiddenError> { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Barnetrygd), behandling.id, Behandlerrolle.FORVALTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `forvalterrolle sammen med veileder burde ikke gi tilgang til hva som helst`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(ENSLIG_VEILEDER_ROLLE, TEAMFAMILIE_FORVALTER_ROLLE))
        opprettRequestContext(token)

        shouldThrow<ForbiddenError> { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Barnetrygd), behandling.id, Behandlerrolle.BESLUTTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `tilleggsstønader - krever beslutter rolle, men har saksbehandler`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(TILLEGSSTØNADER_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldThrow<ForbiddenError> { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Tilleggsstønad), behandling.id, Behandlerrolle.BESLUTTER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `tilleggsstønader - krever saksbehandler rolle, har tilgang`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.Ok
        val token = opprettToken("abc", listOf(TILLEGSSTØNADER_SAKSBEHANDLER_ROLLE))
        opprettRequestContext(token)

        shouldNotThrowAny { tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving(Ytelse.Tilleggsstønad), behandling.id, Behandlerrolle.SAKSBEHANDLER, AuditLoggerEvent.ACCESS, "test") }
    }

    @Test
    fun `tilleggsstønader - krever beslutter rolle, har tilgang`() {
        coEvery { persontilgangService.sjekkPersontilgang(any(), any()) } returns Persontilgang.Ok
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
            features = FeatureToggles(EnumMap(Toggle::class.java)),
        )
    }
}
