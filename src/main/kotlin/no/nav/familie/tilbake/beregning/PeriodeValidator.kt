package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import org.springframework.http.HttpStatus
import java.time.YearMonth

fun validatePerioder(
    perioder: List<Datoperiode>,
    logContext: SecureLog.Context,
) {
    val perioderSomIkkeErHeleMåneder =
        perioder.filter {
            it.fom.dayOfMonth != 1 ||
                it.tom.dayOfMonth != YearMonth.from(it.tom).lengthOfMonth()
        }

    if (perioderSomIkkeErHeleMåneder.isNotEmpty()) {
        throw Feil(
            message = "Periode med ${perioderSomIkkeErHeleMåneder[0]} er ikke i hele måneder",
            frontendFeilmelding = "Periode med ${perioderSomIkkeErHeleMåneder[0]} er ikke i hele måneder",
            httpStatus = HttpStatus.BAD_REQUEST,
            logContext = logContext,
        )
    }
}
