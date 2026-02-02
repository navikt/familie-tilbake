package no.nav.tilbakekreving.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.config.ApplicationProperties
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
@Profile("dev", "prod")
class BigQueryServiceImpl(
    private val environment: Environment,
    private val applicationProperties: ApplicationProperties,
    private val bigQuery: BigQuery,
) : BigQueryService {
    private val logger = TracedLogger.getLogger<BigQueryServiceImpl>()
    private val logContext = SecureLog.Context.tom()
    private val prosjektId = applicationProperties.bigQuery.prosjektId
    private val dataset = applicationProperties.bigQuery.dataset

    override fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    ) {
        val radInnhold = mapOf(
            "tid" to Instant.now().toString(),
            "behandling_id" to bigqueryData.behandlingId,
            "opprettet_tid" to bigqueryData.opprettetDato?.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            ),
            "periode_fom" to bigqueryData.periode?.fom.toString(),
            "periode_tom" to bigqueryData.periode?.tom.toString(),
            "behandlingstype" to bigqueryData.behandlingstype,
            "ytelses_type" to bigqueryData.ytelse,
            "belop" to bigqueryData.beløp,
            "behandlende_enhet_navn" to bigqueryData.enhetNavn,
            "behandlende_enhet_kode" to bigqueryData.enhetKode,
            "status" to bigqueryData.status,
            "resultat" to bigqueryData.resultat,
        )

        val tableId = TableId.of(prosjektId, dataset, "bq_behandling")
        val request = InsertAllRequest.newBuilder(tableId)
            .addRow(radInnhold)
            .build()

        try {
            val response = bigQuery.insertAll(request)
            if (response.hasErrors()) {
                logger.medContext(logContext) {
                    error("Insert til BigQuery feilet: {}", response.insertErrors)
                }
            }
        } catch (e: Exception) {
            logger.medContext(logContext) {
                error("Kunne ikke utføre BigQuery INSERT", e)
            }
        }
    }
}
