package no.nav.tilbakekreving.bigquery

import java.time.LocalDateTime

interface BigQueryService {
    fun leggeTilBehanlingInfo(
        behandlingId: String,
        opprettetTid: LocalDateTime,
        ytelsestypeKode: String,
        behandlingstype: String,
        behandlendeEnhet: String?,
    )
}
