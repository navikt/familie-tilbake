package no.nav.familie.tilbake.common.exceptionhandler

import no.nav.familie.tilbake.log.SecureLog
import org.springframework.http.HttpStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class ApiFeil(
    val feil: String,
    val httpStatus: HttpStatus,
) : RuntimeException()

class Feil(
    message: String,
    val frontendFeilmelding: String? = null,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val logContext: SecureLog.Context,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable) {
    constructor(message: String, throwable: Throwable?, logContext: SecureLog.Context, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR) :
        this(message, null, httpStatus, logContext, throwable)
}

@OptIn(ExperimentalContracts::class)
inline fun feilHvis(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    logContext: SecureLog.Context,
    lazyMessage: () -> String,
) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw Feil(message = lazyMessage(), frontendFeilmelding = lazyMessage(), logContext = logContext, httpStatus = httpStatus)
    }
}

class ManglerOppgaveFeil(
    val melding: String,
) : RuntimeException(melding)

class UgyldigKravgrunnlagFeil(
    val melding: String,
    val logContext: SecureLog.Context,
) : RuntimeException(melding)

class UkjentravgrunnlagFeil(
    val melding: String,
) : RuntimeException(melding)

class UgyldigStatusmeldingFeil(
    val melding: String,
    val logContext: SecureLog.Context,
) : RuntimeException(melding)

class SperretKravgrunnlagFeil(
    val melding: String,
    logContext: SecureLog.Context,
) : IntegrasjonException(melding, logContext)

class KravgrunnlagIkkeFunnetFeil(
    val melding: String,
    logContext: SecureLog.Context,
) : IntegrasjonException(melding, logContext)
