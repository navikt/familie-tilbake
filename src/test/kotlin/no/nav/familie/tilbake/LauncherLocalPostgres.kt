package no.nav.familie.tilbake

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.Properties

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class LauncherLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5432/familie-tilbake"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    System.setProperty("spring.profiles.active", "local, mock-pdl, mock-oauth") //QAD hack for å få riktige profiler til spring 2.4.3
    SpringApplicationBuilder(LauncherLocalPostgres::class.java)
            .profiles("local", "mock-pdl", "mock-oauth")
            .properties(properties)
            .run(*args)
}
