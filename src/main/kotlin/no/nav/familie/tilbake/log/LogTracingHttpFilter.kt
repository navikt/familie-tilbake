package no.nav.familie.tilbake.log

import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import java.io.EOFException
import java.io.IOException
import java.util.UUID

class LogTracingHttpFilter : HttpFilter() {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Throws(ServletException::class, IOException::class)
    override fun doFilter(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val consumerId = httpServletRequest.getHeader(HEADER_CONSUMER_ID)
        val callId = resolveCallId(httpServletRequest)

        putCallId(callId)
        if (!consumerId.isNullOrEmpty()) {
            putConsumerId(consumerId)
        }
        putRequestId(resolveRequestId(httpServletRequest))
        httpServletResponse.setHeader(HEADER_NAV_CALL_ID, callId)
        try {
            filterWithErrorHandling(httpServletRequest, httpServletResponse, filterChain)
        } finally {
            removeCallId()
            removeConsumerId()
            removeRequestId()
        }
    }

    private fun filterWithErrorHandling(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse)
        } catch (e: EOFException) {
            log.warn("Got EOF while handling HTTP request", e)
        } catch (e: Exception) {
            if (httpServletResponse.isCommitted) {
                log.error("Caught exception while handling HTTP request, failed with status={}", httpServletResponse.status, e)
                throw e
            } else {
                log.error("Caught exception while handing HTTP request", e)
            }
            httpServletResponse.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        }
    }

    override fun init(filterConfig: FilterConfig) {}

    override fun destroy() {}

    companion object {
        private const val HEADER_CONSUMER_ID = "Nav-Consumer-Id"
        private const val HEADER_NAV_CALL_ID: String = "Nav-Call-Id"
        private const val HEADER_NGNINX_REQUEST_ID: String = "X-Request-Id"

        private fun resolveCallId(httpServletRequest: HttpServletRequest): String = headerOrRandom(httpServletRequest, HEADER_NAV_CALL_ID, "Nav-CallId", "Nav-Callid", "X-Correlation-Id")

        private fun resolveRequestId(httpServletRequest: HttpServletRequest): String = headerOrRandom(httpServletRequest, "X_Request_Id", HEADER_NGNINX_REQUEST_ID)

        private fun headerOrRandom(
            httpServletRequest: HttpServletRequest,
            vararg headerNames: String,
        ) = headerNames
            .asSequence()
            .mapNotNull(httpServletRequest::getHeader)
            .firstOrNull(String::isNotEmpty)
            ?: UUID.randomUUID().toString()
    }
}
