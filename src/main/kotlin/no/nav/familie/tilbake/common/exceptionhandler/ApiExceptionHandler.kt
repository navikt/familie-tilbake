package no.nav.familie.tilbake.common.exceptionhandler

import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@Suppress("unused")
@ControllerAdvice
class ApiExceptionHandler {
    private val logger = TracedLogger.getLogger<ApiExceptionHandler>()

    private fun rootCause(throwable: Throwable): String = NestedExceptionUtils.getMostSpecificCause(throwable).javaClass.simpleName

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        val logContext = SecureLog.Context.springContext()
        SecureLog.medContext(logContext) { error("En feil har oppstått", throwable) }
        logger.medContext(logContext) { error("En feil har oppstått: {}", rootCause(throwable)) }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(errorMessage = "Uventet feil", frontendFeilmelding = "En uventet feil oppstod."))
    }

    @ExceptionHandler(ApiFeil::class)
    fun handleThrowable(feil: ApiFeil): ResponseEntity<Ressurs<Nothing>> = ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(frontendFeilmelding = feil.feil))

    @ExceptionHandler(Feil::class)
    fun handleThrowable(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        SecureLog.medContext(feil.logContext) {
            warn("En håndtert feil har oppstått({}): {}", feil.httpStatus, feil.message, feil)
        }
        logger.medContext(feil.logContext) {
            info("En håndtert feil har oppstått({}) exception={}: {}", feil.httpStatus, rootCause(feil), feil.message)
        }
        return ResponseEntity.status(feil.httpStatus).body(
            Ressurs.failure(
                errorMessage = feil.message,
                frontendFeilmelding = feil.frontendFeilmelding,
            ),
        )
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleThrowable(feil: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> {
        SecureLog.medContext(feil.logContext) {
            error("Feil i integrasjoner har oppstått: uri={} data={}", feil.uri, feil.data, feil)
        }
        SecureLog.medContext(feil.logContext) {
            error("Feil i integrasjoner har oppstått exception=${rootCause(feil)}")
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(frontendFeilmelding = feil.message, error = feil.cause))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleThrowable(feil: MethodArgumentNotValidException): ResponseEntity<Ressurs<Nothing>> {
        val feilmelding = StringBuilder()
        feil.bindingResult.fieldErrors.forEach { fieldError ->
            SecureLog.utenContext().error(
                "Validering feil har oppstått: field={} message={} verdi={}",
                fieldError.field,
                fieldError.defaultMessage,
                fieldError.rejectedValue,
            )
            logger.medContext(SecureLog.Context.springContext()) {
                error("Validering feil har oppstått: field={} message={}", fieldError.field, fieldError.defaultMessage)
            }
            feilmelding.append(fieldError.defaultMessage)
            feilmelding.append(";")
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Ressurs.failure(errorMessage = feilmelding.toString(), frontendFeilmelding = feilmelding.toString()))
    }

    @ExceptionHandler(UgyldigKravgrunnlagFeil::class)
    fun handleThrowable(feil: UgyldigKravgrunnlagFeil): ResponseEntity<Ressurs<Nothing>> {
        SecureLog.medContext(feil.logContext) { error("En håndtert feil har oppstått - {}", feil.melding, feil) }
        logger.medContext(feil.logContext) { info("En håndtert feil har oppstått - {}", feil.melding) }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(frontendFeilmelding = feil.message))
    }

    @ExceptionHandler(UgyldigStatusmeldingFeil::class)
    fun handleThrowable(feil: UgyldigStatusmeldingFeil): ResponseEntity<Ressurs<Nothing>> {
        SecureLog.medContext(feil.logContext) { error("En håndtert feil har oppstått - {}", feil.melding, feil) }
        logger.medContext(feil.logContext) { info("En håndtert feil har oppstått - {}", feil.melding) }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(frontendFeilmelding = feil.message))
    }
}
