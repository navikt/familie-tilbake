package no.nav.familie.tilbake.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "bigquery.flyway")
data class FlywayBigQueryProps(
    var enabled: Boolean,
    var dataset: String,
    var jdbcUrl: String,
)

@Component
class BigQueryMigrering(
    private val props: FlywayBigQueryProps,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun run() {
        if (!props.enabled) {
            logger.info("BigQuery Flyway: deaktivert")
            return
        }

        val instansId = System.getenv("HOSTNAME") ?: "ukjent"
        logger.info("BigQuery Flyway: starter (instansId=$instansId, dataset=${props.dataset})")

        val resultat = Flyway.configure()
            .driver("com.simba.googlebigquery.jdbc42.Driver")
            .dataSource(props.jdbcUrl, "", "")
            .schemas(props.dataset)
            .locations("classpath:db/migration-bigquery")
            .lockRetryCount(-1)
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate()

        logger.info("BigQuery Flyway: ferdig (instansId=$instansId, migreringer=${resultat.migrationsExecuted})")
    }
}
