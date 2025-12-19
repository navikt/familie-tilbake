package no.tilbakekreving.integrasjoner.feil

import io.ktor.http.HttpStatusCode

class NotFoundException(
    message: String,
    val statusCode: HttpStatusCode,
    val response: String?,
) : Exception(message)
