package no.tilbakekreving.integrasjoner.feil

import io.ktor.http.HttpStatusCode

class UnexpectedResponseException(
    message: String,
    val statusCode: HttpStatusCode,
) : Exception(message)
