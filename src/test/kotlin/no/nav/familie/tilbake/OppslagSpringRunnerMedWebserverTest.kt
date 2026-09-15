package no.nav.familie.tilbake

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

/**
 * For tester som trenger en kjørende webserver (HTTP-kall mot endepunkter).
 * Merk: egendefinert webEnvironment gir en egen Spring-context, så denne skal kun brukes når det er nødvendig.
 */
@SpringBootTest(classes = [LauncherLocal::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class OppslagSpringRunnerMedWebserverTest : OppslagSpringRunnerTest() {
    @LocalServerPort
    private var port: Int? = 0

    protected fun localhost(uri: String): String = "http://localhost:$port$uri"
}
