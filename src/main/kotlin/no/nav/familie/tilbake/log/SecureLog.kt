package no.nav.familie.tilbake.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

object SecureLog {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun medContext(context: Context): Logger {
        try {
            MDC.put("fagsystemId", context.fagsystemId)
            MDC.put("behandlingId", context.behandlingId)
            return secureLogger
        } finally {
            MDC.remove("fagsystemId")
            MDC.remove("behandlingId")
        }
    }

    fun medBehandling(
        fagsystemId: String,
        behandlingId: String?,
    ) = medContext(Context.medBehandling(fagsystemId = fagsystemId, behandlingId = behandlingId))

    fun utenBehandling(fagsystemId: String) = medContext(Context.utenBehandling(fagsystemId = fagsystemId))

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
