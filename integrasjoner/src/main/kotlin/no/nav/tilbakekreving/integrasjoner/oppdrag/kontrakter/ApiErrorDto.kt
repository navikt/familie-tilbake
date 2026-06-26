package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

import java.time.OffsetDateTime

data class ApiErrorDto(
    val timestamp: OffsetDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
)
