package no.nav.familie.tilbake.http

import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.util.UUID

class MdcValuesPropagatingClientInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val callId = MDC.get("callId") ?: UUID.randomUUID().toString()
        val requestId = MDC.get("requestId") ?: callId
        request.headers.add("Nav-Call-Id", callId)
        request.headers.add("X-Request-ID", requestId)

        return execution.execute(request, body)
    }
}
