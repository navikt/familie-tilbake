package no.nav.tilbakekreving.bigquery

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Profile("e2e", "local", "integrasjonstest")
@Service
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
