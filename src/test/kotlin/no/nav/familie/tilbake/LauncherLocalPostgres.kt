package no.nav.familie.tilbake

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.*

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class LauncherLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5432/familie-tilbake"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    SpringApplicationBuilder(LauncherLocalPostgres::class.java)
            .profiles("local",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-oppdrag",
                      "mock-kodeverk")
            .properties(properties)
            .run(*args)
}