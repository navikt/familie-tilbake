package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto

interface BigQueryService {
    fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    )
}
