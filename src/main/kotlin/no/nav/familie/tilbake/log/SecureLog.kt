package no.nav.familie.tilbake.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

object SecureLog {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun medContext(context: Context): Logger {
        try {
            MDC.put("fagsakId", context.fagsakId)
            MDC.put("behandlingId", context.behandlingId)
            return secureLogger
        } finally {
            MDC.remove("fagsakId")
            MDC.remove("behandlingId")
        }
    }

    fun medBehandlings(
        fagsakId: String,
        behandlingId: String?,
    ) = medContext(Context.medBehandling(fagsakId = fagsakId, behandlingId = behandlingId))

    fun utenBehandling(fagsakId: String) = medContext(Context.utenBehandling(fagsakId = fagsakId))

    fun utenContext() = secureLogger

    data class Context private constructor(
        val fagsakId: String,
        val behandlingId: String,
    ) {
        companion object {
            fun tom() = utenBehandling("ukjent")

            fun utenBehandling(fagsakId: String) = medBehandling(fagsakId = fagsakId, behandlingId = null)

            fun medBehandling(
                fagsakId: String,
                behandlingId: String?,
            ) = Context(fagsakId = fagsakId, behandlingId = behandlingId ?: "ukjent")
        }
    }
}
