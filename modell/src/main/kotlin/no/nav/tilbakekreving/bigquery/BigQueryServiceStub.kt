package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto

class BigQueryServiceStub : BigQueryService {
    @Override
    override fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    ) {}
}
