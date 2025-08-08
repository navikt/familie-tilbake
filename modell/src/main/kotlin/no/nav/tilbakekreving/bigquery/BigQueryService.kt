package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.behandling.Behandling

interface BigQueryService {
    fun leggeTilBehanlingInfo(
        behandling: Behandling,
    )
}
