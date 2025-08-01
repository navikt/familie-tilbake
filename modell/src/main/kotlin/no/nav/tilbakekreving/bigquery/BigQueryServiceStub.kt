package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.behandling.Behandling

class BigQueryServiceStub : BigQueryService {
    @Override
    override fun leggeTilBehanlingInfo(
        behandling: Behandling,
    ) {}
}
