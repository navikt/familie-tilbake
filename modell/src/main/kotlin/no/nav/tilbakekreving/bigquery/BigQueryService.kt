package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto

interface BigQueryService {
    fun oppdaterBehandling(
        bigqueryData: BigQueryBehandlingDataDto,
    )

    fun lagreVilkårsvurdering(bigqueryData: BigQueryVilkårsvurderingDataDto)
}
