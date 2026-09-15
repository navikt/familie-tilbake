package no.nav.familie.tilbake

import no.nav.familie.tilbake.database.DbContainerInitializer
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.jdbc.core.JdbcAggregateOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigInteger

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootTest(classes = [LauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("integrasjonstest", "mock-pdl", "mock-integrasjoner", "mock-oppgave", "mock-økonomi")
abstract class OppslagSpringRunnerTest {
    @Autowired
    private lateinit var jdbcAggregateOperations: JdbcAggregateOperations

    @Autowired
    private lateinit var applicationContext: ApplicationContext

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
}
