package no.nav.familie.tilbake

import no.nav.familie.tilbake.config.ApplicationConfig
import no.nav.familie.tilbake.database.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class LauncherLocal

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active",
                       "local, mock-pdl, mock-oauth, mock-oppgave") //QAD hack for å få riktige profiler til spring 2.4.3
    SpringApplicationBuilder(ApplicationConfig::class.java)
            .initializers(DbContainerInitializer())
            .profiles("local", "mock-pdl", "mock-oauth",  "mock-oppgave")
            .run(*args)
}
