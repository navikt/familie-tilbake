package no.nav.familie.tilbake.sikkerhet

import io.jsonwebtoken.Jwts
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.aspectj.lang.JoinPoint
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TilgangAdviceTest : OppslagSpringRunnerTest() {

    companion object {

        const val BARNETRYGD_BESLUTTER_ROLLE = "bb123"
        const val BARNETRYGD_SAKSBEHANDLER_ROLLE = "bs123"
        const val BARNETRYGD_VEILEDER_ROLLE = "bv123"

        const val ENSLIG_BESLUTTER_ROLLE = "eb123"
        const val ENSLIG_SAKSBEHANDLER_ROLLE = "es123"
        const val ENSLIG_VEILEDER_ROLLE = "ev123"

        const val KONTANTSTØTTE_BESLUTTER_ROLLE = "kb123"
        const val KONTANTSTØTTE_SAKSBEHANDLER_ROLLE = "ks123"
        const val KONTANTSTØTTE_VEILEDER_ROLLE = "kv123"
    }


    @Autowired
    private lateinit var tilgangAdvice: TilgangAdvice

    private val mockJoinpoint: JoinPoint = mock(JoinPoint::class.java)
    private val mockRolleTilgangssjekk = mock(RolleTilgangssjekk::class.java)

    @BeforeEach
    fun init() {
        val rolleConfig = tilgangAdvice.rolleConfig
        ReflectionTestUtils.setField(rolleConfig, "environmentName", "test")
        ReflectionTestUtils.setField(rolleConfig, "beslutterRolleBarnetrygd", BARNETRYGD_BESLUTTER_ROLLE)
        ReflectionTestUtils.setField(rolleConfig, "saksbehandlerRolleBarnetrygd", BARNETRYGD_SAKSBEHANDLER_ROLLE)
        ReflectionTestUtils.setField(rolleConfig, "veilederRolleBarnetrygd", BARNETRYGD_VEILEDER_ROLLE)

        ReflectionTestUtils.setField(rolleConfig, "beslutterRolleEnslig", ENSLIG_BESLUTTER_ROLLE)
        ReflectionTestUtils.setField(rolleConfig, "saksbehandlerRolleEnslig", ENSLIG_SAKSBEHANDLER_ROLLE)
        ReflectionTestUtils.setField(rolleConfig, "veilederRolleEnslig", ENSLIG_VEILEDER_ROLLE)

        ReflectionTestUtils.setField(rolleConfig, "beslutterRolleKontantStøtte", KONTANTSTØTTE_BESLUTTER_ROLLE)
        ReflectionTestUtils.setField(rolleConfig, "saksbehandlerRolleKontantStøtte", KONTANTSTØTTE_SAKSBEHANDLER_ROLLE)
        ReflectionTestUtils.setField(rolleConfig, "veilederRolleKontantStøtte", KONTANTSTØTTE_VEILEDER_ROLLE)
    }

    @Test
    fun `sjekkTilgang skal ha tilgang for barnetrygd beslutter i barnetrygd hent behandling request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", BARNETRYGD_BESLUTTER_ROLLE)
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        `when`(mockJoinpoint.args).thenReturn(arrayOf(behandlingId))
        `when`(mockRolleTilgangssjekk.minimumBehandlerRolle).thenReturn(BehandlerRolle.VEILEDER)
        `when`(mockRolleTilgangssjekk.handling).thenReturn("hent behandling")
        `when`(mockRolleTilgangssjekk.henteParam).thenReturn("behandlingId")

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for enslig beslutter i barnetrygd hent behandling request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", ENSLIG_BESLUTTER_ROLLE)
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        `when`(mockJoinpoint.args).thenReturn(arrayOf(behandlingId))
        `when`(mockRolleTilgangssjekk.minimumBehandlerRolle).thenReturn(BehandlerRolle.VEILEDER)
        `when`(mockRolleTilgangssjekk.handling).thenReturn("barnetrygd hent behandling")
        `when`(mockRolleTilgangssjekk.henteParam).thenReturn("behandlingId")

        assertFailsWith<RuntimeException>(message = "abc med ENSLIG_FORELDER tilgang har ikke tilgang " +
                                                    "til barnetrygd hent behandling",
                                          block = { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) })

        val exception = assertFailsWith<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       mockRolleTilgangssjekk)
        })
        assertEquals("abc med ENSLIG_FORELDER tilgang har ikke tilgang til ${mockRolleTilgangssjekk.handling}",
                     exception.message)
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for barnetrygd veileder i barnetrygd opprett behandling request`() {
        val varsel = Varsel("hello", BigDecimal.valueOf(1000), emptyList())
        val opprettBehandlingRequest =
                OpprettTilbakekrevingRequest(ytelsestype = no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD,
                                             eksternFagsakId = "123",
                                             personIdent = PersonIdent("123434"),
                                             eksternId = "123",
                                             manueltOpprettet = false,
                                             enhetId = "8020",
                                             enhetsnavn = "Oslo",
                                             revurderingsvedtaksdato = LocalDate.now(),
                                             varsel = varsel)
        val token = opprettToken("abc", BARNETRYGD_VEILEDER_ROLLE)
        opprettRequest("/api/behandling/v1", HttpMethod.POST, token)

        `when`(mockJoinpoint.args).thenReturn(arrayOf(opprettBehandlingRequest))
        `when`(mockRolleTilgangssjekk.minimumBehandlerRolle).thenReturn(BehandlerRolle.SAKSBEHANDLER)
        `when`(mockRolleTilgangssjekk.handling).thenReturn("barnetrygd opprett behandling")

        val exception = assertFailsWith<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       mockRolleTilgangssjekk)
        })
        assertEquals("abc med rolle VEILEDER har ikke tilgang til å barnetrygd opprett behandling. " +
                     "Krever ${mockRolleTilgangssjekk.minimumBehandlerRolle}.",
                     exception.message)
    }


    private fun opprettBehandling(ytelsestype: Ytelsestype): Behandling {
        val fagsak = Fagsak(bruker = Bruker("1232"),
                            eksternFagsakId = "123",
                            fagsystem = Fagsystem.fraYtelsestype(ytelsestype),
                            ytelsestype = ytelsestype,
                            status = Fagsaksstatus.OPPRETTET)
        tilgangAdvice.fagsakRepository.insert(fagsak)
        val behandling = Behandling(fagsakId = fagsak.id,
                                    type = Behandlingstype.TILBAKEKREVING,
                                    ansvarligSaksbehandler = "VL",
                                    behandlendeEnhet = "8020",
                                    behandlendeEnhetsNavn = "Oslo",
                                    manueltOpprettet = false)
        tilgangAdvice.behandlingRepository.insert(behandling)
        return behandling
    }

    private fun opprettToken(behandlerNavn: String, gruppeNavn: String): String {
        val additionalParameters = mapOf("preferred_username" to behandlerNavn, "groups" to listOf(gruppeNavn))
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, 60)
        return Jwts.builder().setExpiration(calendar.time)
                .setIssuer("azuread")
                .addClaims(additionalParameters).compact()

    }

    private fun opprettRequest(requestUri: String, requestMethod: HttpMethod, token: String) {
        val mockHttpServletRequest =
                MockHttpServletRequest(requestMethod.name, requestUri)

        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockHttpServletRequest))
        val tokenValidationContext = TokenValidationContext(mapOf<String, JwtToken>
                                                            ("azuread" to JwtToken(token)))
        RequestContextHolder.currentRequestAttributes()
                .setAttribute(SpringTokenValidationContextHolder::class.java.name, tokenValidationContext, 0)
    }


}
