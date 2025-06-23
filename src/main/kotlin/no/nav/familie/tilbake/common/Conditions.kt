package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog

fun <T> List<T>.expectSingleOrNull(
    logContext: SecureLog.Context,
    onFailure: (T) -> String,
): T? {
    return when {
        isEmpty() -> null
        size == 1 -> return single()
        else -> throw Feil("Forventet et innslag i tabellen. Fant ${joinToString(", ", transform = onFailure)}.", logContext = logContext)
    }
}
