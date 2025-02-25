package no.nav.familie.tilbake.client

import no.nav.familie.tilbake.kontrakter.Ressurs
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException

class RessursException(
    val ressurs: Ressurs<Any>,
    cause: RestClientResponseException,
    val httpStatus: HttpStatus = HttpStatus.valueOf(cause.rawStatusCode),
) : RuntimeException(cause)
