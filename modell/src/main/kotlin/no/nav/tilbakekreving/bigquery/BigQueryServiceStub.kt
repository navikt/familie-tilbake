package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto

class BigQueryServiceStub : BigQueryService {
    @Override
    override fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    ) {}

    override fun lagreVilkårsvurdering(bigqueryData: BigQueryVilkårsvurderingDataDto) {}
}
