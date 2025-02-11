package no.nav.familie.tilbake.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class TracedLogger(
    private val log: Logger,
) {
    fun medContext(
        context: SecureLog.Context,
        callback: Logger.() -> Unit,
    ) {
        try {
            MDC.put("fagsystemId", context.fagsystemId)
            MDC.put("behandlingId", context.behandlingId)
            return log.callback()
        } finally {
            MDC.remove("fagsystemId")
            MDC.remove("behandlingId")
        }
    }

    companion object {
        inline fun <reified L> getLogger() = TracedLogger(LoggerFactory.getLogger(L::class.java))
    }
}
