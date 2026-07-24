package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryTilleggsfristDto
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("e2e", "local", "integrasjonstest")
@Service
class BigQueryServiceStub : BigQueryService {
    @Override
    override fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    ) {}

    override fun loggTilleggsfrist(data: BigQueryTilleggsfristDto) {}
}
