package no.nav.tilbakekreving.integrasjoner.oppdrag

import java.time.OffsetDateTime

data class ApiErrorDto(
    val timestamp: OffsetDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
)
