package no.nav.familie.tilbake.sikkerhet

import jakarta.servlet.http.HttpServletRequest
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.callId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Component
class NaisAuditLogger(
    @Value("\${NAIS_APP_NAME:appName}") private val applicationName: String,
) : AuditLogger {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val audit = LoggerFactory.getLogger("auditLogger")

    override fun log(data: Sporingsdata) {
        val request = getRequest() ?: throw IllegalArgumentException("Ikke brukt i context av en HTTP request")

        if (!ContextService.erMaskinTilMaskinToken()) {
            audit.info(createAuditLogString(data, request))
        } else {
            logger.debug("Maskin til maskin token i request")
        }
    }

    private fun getRequest(): HttpServletRequest? =
        RequestContextHolder
            .getRequestAttributes()
            ?.takeIf { it is ServletRequestAttributes }
            ?.let { it as ServletRequestAttributes }
            ?.request

    private fun createAuditLogString(
        data: Sporingsdata,
        request: HttpServletRequest,
    ): String {
        val timestamp = System.currentTimeMillis()
        val name = "Saksbehandling"
        return "CEF:0|Familie|$applicationName|1.0|audit:${data.event.type}|$name|INFO|end=$timestamp " +
            "suid=${ContextService.hentSaksbehandler(SecureLog.Context.tom())} " +
            "duid=${data.personIdent} " +
            "sproc=${getCallId()} " +
            "requestMethod=${request.method} " +
            "request=${request.requestURI} " +
            createCustomString(data)
    }

    private fun createCustomString(data: Sporingsdata): String =
        listOfNotNull(
            data.custom1?.let { "cs3Label=${it.key} cs3=${it.value}" },
            data.custom2?.let { "cs5Label=${it.key} cs5=${it.value}" },
            data.custom3?.let { "cs6Label=${it.key} cs6=${it.value}" },
        ).joinToString(" ")

    private fun getCallId(): String = callId() ?: throw IllegalStateException("Mangler callId")
}
