package no.nav.familie.tilbake.log

import no.nav.familie.prosessering.domene.Task
import no.nav.tilbakekreving.Tilbakekreving
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.Properties

object SecureLog {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun medContext(
        context: Context,
        callback: Logger.() -> Unit,
    ) {
        try {
            MDC.put("fagsystemId", context.fagsystemId ?: "ukjent")
            MDC.put("behandlingId", context.behandlingId ?: "ukjent")
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
        val fagsystemId: String?,
        val behandlingId: String?,
    ) {
        fun copyTo(properties: Properties) {
            properties.setProperty("logContext.fagsystemId", fagsystemId ?: "ukjent")
            properties.setProperty("logContext.behandlingId", behandlingId ?: "ukjent")
        }

        companion object {
            fun tom() = utenBehandling(null)

            fun utenBehandling(fagsystemId: String?) = medBehandling(fagsystemId = fagsystemId, behandlingId = null)

            fun medBehandling(
                fagsystemId: String?,
                behandlingId: String?,
            ) = Context(fagsystemId = fagsystemId, behandlingId = behandlingId)

            fun fra(tilbakekreving: Tilbakekreving) =
                medBehandling(
                    fagsystemId = tilbakekreving.eksternFagsak.eksternId,
                    behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.internId.toString(),
                )

            fun Task.logContext(): Context =
                medBehandling(
                    metadata.getProperty("logContext.fagsystemId"),
                    metadata.getProperty("logContext.behandlingId"),
                )
        }
    }
}
