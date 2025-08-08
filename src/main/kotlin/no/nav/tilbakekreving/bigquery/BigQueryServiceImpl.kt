package no.nav.tilbakekreving.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableId
import jakarta.annotation.PostConstruct
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.config.ApplicationProperties
import org.springframework.core.env.Environment
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class BigQueryServiceImpl(
    private val environment: Environment,
    private val applicationProperties: ApplicationProperties,
    private val bigQuery: BigQuery,
    private val resourceResolver: ResourcePatternResolver = PathMatchingResourcePatternResolver(),
) : BigQueryService {
    private val logger = TracedLogger.getLogger<BigQueryServiceImpl>()
    private val logContext = SecureLog.Context.tom()
    private val prosjektId = applicationProperties.bigQuery.prosjektId
    private val dataset = applicationProperties.bigQuery.dataset
    private val behandlingTable = applicationProperties.bigQuery.behandlingTable

    override fun leggeTilBehanlingInfo(
        behandling: Behandling,
    ) {
        val info = behandling.hentBehandlingsinformasjon()

        val radInnhold = mapOf(
            "behandling_id" to info.behandlingId.toString(),
            "opprettet_tid" to info.opprettetTid.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            "behandlingstype" to info.behandlingstype.name,
            "behandlende_enhet" to info.enhet?.navn,
        )

        val tableId = TableId.of(prosjektId, dataset, behandlingTable)
        val request = InsertAllRequest.newBuilder(tableId)
            .addRow(radInnhold)
            .build()

        try {
            val response = bigQuery.insertAll(request)
            if (response.hasErrors()) {
                logger.medContext(logContext) {
                    error("Insert til BigQuery feilet: ${response.insertErrors}")
                }
            } else {
                logger.medContext(logContext) {
                    info("Raden ble satt inn i BigQuery med hell.")
                }
            }
        } catch (e: Exception) {
            logger.medContext(logContext) {
                error("Kunne ikke utføre BigQuery INSERT", e)
            }
        }
    }

    @PostConstruct
    fun runBigQueryMigrations() {
        val isProd = environment.activeProfiles.contains("prod")
        val isDev = environment.activeProfiles.contains("dev")
        if (isProd || isDev) {
            val resources = resourceResolver.getResources("classpath:/bigquery/migration/*.sql")

            for (resource in resources.sortedBy { it.filename }) {
                val query = resource.inputStream.bufferedReader().use { it.readText() }

                logger.medContext(logContext) {
                    info("Kjører BigQuery migrering: ${resource.filename}")
                }
                val config = QueryJobConfiguration.newBuilder(query).build()
                val job = bigQuery.create(JobInfo.of(config))
                val completedJob = job.waitFor()

                val error = completedJob.status.error
                if (error != null) {
                    throw RuntimeException("Feil i migrering ${resource.filename}: ${error.message}")
                }
                logger.medContext(logContext) {
                    info("Fullførte BigQuery migrering: ${resource.filename}")
                }
            }
        }
    }
}
