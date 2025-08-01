package no.nav.tilbakekreving.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import no.nav.tilbakekreving.config.ApplicationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BigQueryConfig(private val applicationProperties: ApplicationProperties) {
    @Bean
    fun bigQuery(): BigQuery {
        return BigQueryOptions.newBuilder()
            .setProjectId(applicationProperties.bigQuery.prosjektId)
            .build()
            .service
    }
}
