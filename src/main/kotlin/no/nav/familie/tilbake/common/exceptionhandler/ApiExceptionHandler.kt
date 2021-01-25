package no.nav.familie.tilbake.common.exceptionhandler

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@Suppress("unused")
@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private fun rootCause(throwable: Throwable): String {
        return NestedExceptionUtils.getMostSpecificCause(throwable).javaClass.simpleName
    }

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En feil har oppstått", throwable)
        logger.error("En feil har oppstått: ${rootCause(throwable)} ")

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Ressurs.failure(errorMessage = "Uventet feil", frontendFeilmelding = "En uventet feil oppstod."))
    }

    @ExceptionHandler(ApiFeil::class)
    fun handleThrowable(feil: ApiFeil): ResponseEntity<Ressurs<Nothing>> {
        return ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(frontendFeilmelding = feil.feil))
    }

    @ExceptionHandler(Feil::class)
    fun handleThrowable(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.frontendFeilmelding}", feil)
        logger.info("En håndtert feil har oppstått(${feil.httpStatus}) exception=${rootCause(feil)}: ${feil.message} ")
        return ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(frontendFeilmelding = feil.frontendFeilmelding))
    }

    @ExceptionHandler(ManglerTilgang::class)
    fun handleThrowable(manglerTilgang: ManglerTilgang): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En håndtert tilgangsfeil har oppstått - ${manglerTilgang.melding}", manglerTilgang)
        logger.info("En håndtert tilgangsfeil har oppstått")
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Ressurs.ikkeTilgang(melding = manglerTilgang.melding))
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleThrowable(feil: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("Feil mot integrasjonsclienten har oppstått: uri={} data={}", feil.uri, feil.data, feil)
        logger.error("Feil mot integrasjonsclienten har oppstått exception=${rootCause(feil)}")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Ressurs.failure(frontendFeilmelding = feil.message))
    }

}
