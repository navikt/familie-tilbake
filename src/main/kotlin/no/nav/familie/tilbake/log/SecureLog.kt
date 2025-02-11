package no.nav.familie.tilbake.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

object SecureLog {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun medContext(
        context: Context,
        callback: Logger.() -> Unit,
    ) {
        try {
            MDC.put("fagsystemId", context.fagsystemId)
            MDC.put("behandlingId", context.behandlingId)
            return secureLogger.callback()
        } finally {
            MDC.remove("fagsystemId")
            MDC.remove("behandlingId")
        }
    }

    fun medBehandling(
        fagsystemId: String,
        behandlingId: String?,
        callback: Logger.() -> Unit,
    ) = medContext(Context.medBehandling(fagsystemId = fagsystemId, behandlingId = behandlingId), callback)

    fun utenBehandling(
        fagsystemId: String,
        callback: Logger.() -> Unit,
    ) = medContext(Context.utenBehandling(fagsystemId = fagsystemId), callback)

    fun utenContext() = secureLogger

    data class Context private constructor(
        val fagsystemId: String,
        val behandlingId: String,
    ) {
        companion object {
            fun tom() = utenBehandling("ukjent")

            fun utenBehandling(fagsystemId: String) = medBehandling(fagsystemId = fagsystemId, behandlingId = null)

            fun medBehandling(
                fagsystemId: String,
                behandlingId: String?,
            ) = Context(fagsystemId = fagsystemId, behandlingId = behandlingId ?: "ukjent")
        }
    }
}
