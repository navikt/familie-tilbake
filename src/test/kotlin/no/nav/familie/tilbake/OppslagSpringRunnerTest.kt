package no.nav.familie.tilbake

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.familie.tilbake.database.DbContainerInitializer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tilbakekreving.e2e.ContextServiceHelpers.E2E_TILGANG_GRUPPE
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [LauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrasjonstest", "mock-oauth", "mock-pdl", "mock-integrasjoner", "mock-oppgave", "mock-økonomi")
@EnableMockOAuth2Server
abstract class OppslagSpringRunnerTest {
    private val listAppender = initLoggingEventListAppender()
    protected var loggingEvents: MutableList<ILoggingEvent> = listAppender.list

    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @LocalServerPort
    private var port: Int? = 0

    @AfterEach
    @Transactional
    fun reset() {
        loggingEvents.clear()
        resetWiremockServers()
    }

    fun authorizationHeaders(
        ident: String = SAKSBEHANDLER_IDENT,
        grupper: List<String> = listOf(E2E_TILGANG_GRUPPE),
    ): HttpHeaders {
        return HttpHeaders().apply {
            val claims = buildMap {
                put("NAVident", ident)
                if (grupper.isNotEmpty()) put("groups", grupper)
            }
            setBearerAuth(mockOAuth2Server.issueToken("issuer1", audience = "aud-localhost", claims = claims).serialize())
        }
    }

    protected fun localhost(uri: String): String = LOCALHOST + getPort() + uri

    fun readXml(fileName: String): String {
        val url = requireNotNull(this::class.java.getResource(fileName)) { "fil med filnavn=$fileName finnes ikke" }
        return url.readText()
    }

    fun readKravgrunnlagXmlMedIkkeForeldetDato(
        fileName: String,
        fagsystemId: String,
        kravgrunnlagId: String = KravgrunnlagGenerator.nextId(6),
    ): String = readXml(fileName)
        .konverterDatoIXMLTilIkkeForeldet()
        .replace("<urn:fagsystemId>testverdi</urn:fagsystemId>", "<urn:fagsystemId>$fagsystemId</urn:fagsystemId>")
        .replace("<urn:kravgrunnlagId>0</urn:kravgrunnlagId>", "<urn:kravgrunnlagId>${BigInteger(kravgrunnlagId)}</urn:kravgrunnlagId>")

    private fun resetWiremockServers() {
        applicationContext.getBeansOfType(WireMockServer::class.java).values.forEach(WireMockServer::resetRequests)
    }

    protected fun getPort(): String = port.toString()

    companion object {
        private const val LOCALHOST = "http://localhost:"

        protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
            val listAppender = ListAppender<ILoggingEvent>()
            listAppender.start()
            return listAppender
        }
    }
}
