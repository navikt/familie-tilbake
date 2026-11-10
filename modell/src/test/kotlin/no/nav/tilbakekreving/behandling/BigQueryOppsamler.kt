package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto
import no.nav.tilbakekreving.bigquery.BigQueryService

internal fun SideeffektContext.medBigQuery(bigQuery: BigQueryService) = SideeffektContext(
    behandler,
    endringObservatør,
    behovObservatør,
    bigQuery,
    features,
    klokke,
    behandlingslogg,
)

internal class BigQueryOppsamler : BigQueryService {
    val vurderinger = mutableListOf<BigQueryVilkårsvurderingDataDto>()
    val behandlinger = mutableListOf<BigQueryBehandlingDataDto>()

    override fun lagreVilkårsvurdering(bigqueryData: BigQueryVilkårsvurderingDataDto) {
        vurderinger.add(bigqueryData)
    }

    override fun oppdaterBehandling(bigqueryData: BigQueryBehandlingDataDto) {
        behandlinger.add(bigqueryData)
    }
}
