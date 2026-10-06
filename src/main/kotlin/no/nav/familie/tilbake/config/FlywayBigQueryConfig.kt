package no.nav.familie.tilbake.config

import com.google.cloud.bigquery.jdbc.DataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

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

    private val migrering: CompletableFuture<Unit> = CompletableFuture.supplyAsync(::migrer, Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().factory()))

    @EventListener(ApplicationReadyEvent::class)
    fun ventPåMigrering() {
        migrering.join()
    }

    private fun migrer() {
        if (!props.enabled) {
            logger.info("BigQuery Flyway: deaktivert")
            return
        }
        val instansId = System.getenv("HOSTNAME") ?: "ukjent"
        logger.info("BigQuery Flyway: starter (instansId={}, dataset={})", instansId, props.dataset)

        val resultat = Flyway.configure()
            .dataSource(
                DataSource.fromUrl(props.jdbcUrl).apply {
                    enableSession = true
                    location = "europe-north1"
                },
            )
            .schemas(props.dataset)
            .locations("classpath:db/migration-bigquery")
            .lockRetryCount(-1)
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate()

        logger.info("BigQuery Flyway: ferdig (instansId={}, migreringer={})", instansId, resultat.migrationsExecuted)
    }
}
