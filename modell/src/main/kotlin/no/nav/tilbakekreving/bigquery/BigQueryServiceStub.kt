package no.nav.tilbakekreving.bigquery

import java.time.LocalDateTime

class BigQueryServiceStub : BigQueryService {
    @Override
    override fun leggeTilBehanlingInfo(
        behandlingId: String,
        opprettetTid: LocalDateTime,
        ytelsestypeKode: String,
        behandlingstype: String,
        behandlendeEnhet: String?,
    ) {}
}
