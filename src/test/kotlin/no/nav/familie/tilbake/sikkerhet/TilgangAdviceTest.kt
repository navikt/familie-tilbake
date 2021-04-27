package no.nav.familie.tilbake.sikkerhet

import io.jsonwebtoken.Jwts
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.aspectj.lang.JoinPoint
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.context.TestPropertySource
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@TestPropertySource(properties = ["rolle.barnetrygd.beslutter=bb123",
    "rolle.barnetrygd.saksbehandler=bs123",
    "rolle.barnetrygd.veileder=bv123",
    "rolle.enslig.beslutter=eb123",
    "rolle.enslig.saksbehandler=es123",
    "rolle.enslig.veileder=ev123",
    "rolle.kontantstøtte.beslutter = kb123",
    "rolle.kontantstøtte.saksbehandler = ks123",
    "rolle.kontantstøtte.veileder = kv123"])
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

    @AfterEach
    fun tearDown() {
        RequestContextHolder.currentRequestAttributes().removeAttribute(SpringTokenValidationContextHolder::class.java.name, 0)
    }


    @Autowired
    private lateinit var tilgangAdvice: TilgangAdvice

    private val mockJoinpoint: JoinPoint = mockk()
    private val mockRolleTilgangssjekk: Rolletilgangssjekk = mockk()

    @Test
    fun `sjekkTilgang skal ha tilgang for barnetrygd beslutter i barnetrygd hent behandling request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE))
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.VEILEDER
        every { mockRolleTilgangssjekk.handling } returns "hent behandling"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for enslig beslutter i barnetrygd hent behandling request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(ENSLIG_BESLUTTER_ROLLE))
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.VEILEDER
        every { mockRolleTilgangssjekk.handling } returns "barnetrygd hent behandling"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        assertFailsWith<RuntimeException>(message = "abc har ikke tilgang til barnetrygd hent behandling",
                                          block = { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) })

        val exception = assertFailsWith<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       mockRolleTilgangssjekk)
        })
        assertEquals("abc har ikke tilgang til ${mockRolleTilgangssjekk.handling}",
                     exception.message)
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang for barnetrygd veileder i barnetrygd opprett behandling request`() {
        val token = opprettToken("abc", listOf(BARNETRYGD_VEILEDER_ROLLE))
        opprettRequest("/api/behandling/v1", HttpMethod.POST, token)

        every { mockJoinpoint.args } returns arrayOf(lagOpprettTilbakekrevingRequest())
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.SAKSBEHANDLER
        every { mockRolleTilgangssjekk.handling } returns "barnetrygd opprett behandling"
        every { mockRolleTilgangssjekk.henteParam } returns ""

        val exception = assertFailsWith<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       mockRolleTilgangssjekk)
        })
        assertEquals("abc med rolle VEILEDER har ikke tilgang til å barnetrygd opprett behandling. " +
                     "Krever ${mockRolleTilgangssjekk.minimumBehandlerrolle}.",
                     exception.message)
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i barnetrygd opprett behandling request når bruker både er beslutter og veileder`() {
        val token = opprettToken("abc", listOf(BARNETRYGD_BESLUTTER_ROLLE, BARNETRYGD_VEILEDER_ROLLE))
        opprettRequest("/api/behandling/v1", HttpMethod.POST, token)

        every { mockJoinpoint.args } returns arrayOf(lagOpprettTilbakekrevingRequest())
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.SAKSBEHANDLER
        every { mockRolleTilgangssjekk.handling } returns "barnetrygd opprett behandling"
        every { mockRolleTilgangssjekk.henteParam } returns ""

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når saksbehandler har tilgang til enslig og barnetrygd`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(ENSLIG_SAKSBEHANDLER_ROLLE, BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.VEILEDER
        every { mockRolleTilgangssjekk.handling } returns "hent behandling"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i hent behandling request når bruker er fagsystem`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("VL", listOf())
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.VEILEDER
        every { mockRolleTilgangssjekk.handling } returns "hent behandling"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ikke ha tilgang i hent behandling request når bruker er ukjent`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf())
        opprettRequest("/api/behandling/v1/$behandlingId", HttpMethod.GET, token)

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.VEILEDER
        every { mockRolleTilgangssjekk.handling } returns "hent behandling"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        val exception = assertFailsWith<RuntimeException>(block = {
            tilgangAdvice.sjekkTilgang(mockJoinpoint,
                                       mockRolleTilgangssjekk)
        })
        assertEquals("Bruker har mangler tilgang til hent behandling",
                     exception.message)
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i behandleSteg POST request uten body`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        // POST request uten body
        opprettRequest("/api/behandling/$behandlingId/steg/v1/", HttpMethod.POST, token)

        every { mockJoinpoint.args } returns arrayOf(behandlingId)
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.SAKSBEHANDLER
        every { mockRolleTilgangssjekk.handling } returns "Håndterer behandlingens aktiv steg og fortsetter den til neste steg"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }

    @Test
    fun `sjekkTilgang skal ha tilgang i sett behandling på vent PUT request`() {
        val behandling = opprettBehandling(Ytelsestype.BARNETRYGD)
        val behandlingId = behandling.id
        val token = opprettToken("abc", listOf(BARNETRYGD_SAKSBEHANDLER_ROLLE))
        opprettRequest("/api/behandling/$behandlingId/vent/v1/", HttpMethod.PUT, token)

        every { mockJoinpoint.args } returns arrayOf(behandling.id,
                                                     BehandlingPåVentDto(venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                                         tidsfrist = LocalDate.now().plusWeeks(2)))
        every { mockRolleTilgangssjekk.minimumBehandlerrolle } returns Behandlerrolle.SAKSBEHANDLER
        every { mockRolleTilgangssjekk.handling } returns "Setter behandling på vent"
        every { mockRolleTilgangssjekk.henteParam } returns "behandlingId"

        assertDoesNotThrow { tilgangAdvice.sjekkTilgang(mockJoinpoint, mockRolleTilgangssjekk) }
    }


    private fun opprettBehandling(ytelsestype: Ytelsestype): Behandling {
        val fagsak = Fagsak(bruker = Bruker("1232"),
                            eksternFagsakId = "123",
                            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
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

    private fun opprettToken(behandlerNavn: String, gruppeNavn: List<String>): String {
        val additionalParameters = mapOf("preferred_username" to behandlerNavn, "groups" to gruppeNavn)
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
                                            faktainfo = faktainfo)
    }


}
