package no.nav.familie.tilbake

import no.nav.familie.tilbake.config.ApplicationConfig
import org.springframework.boot.builder.SpringApplicationBuilder
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

object DevPostgresLauncher {
    @JvmStatic
    fun main(args: Array<String>) {

        val psql = KPostgreSQLContainer("postgres")
                .withDatabaseName("familie-tilbake")
                .withUsername("postgres")
                .withPassword("test")

        psql.start()

        val properties = Properties()
        properties["SPRING_DATASOURCE_URL_OVERRIDE"] = psql.jdbcUrl
        properties["SPRING_DATASOURCE_USERNAME_OVERRIDE"] = psql.username
        properties["SPRING_DATASOURCE_PASSWORD_OVERRIDE"] = psql.password
        properties["SPRING_DATASOURCE_DRIVER_OVERRIDE"] = "org.postgresql.Driver"

        SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles("dev_postgres")
                .properties(properties)
                .run(*args)
    }
}

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
