import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import no.nav.familie.tilbake.http.RessursException
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.kontrakter.Ressurs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class AbstractPingableRestClient(
    val operations: RestOperations,
    metricsPrefix: String,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val responstid: Timer = Metrics.timer("$metricsPrefix.tid")
    private val responsSuccess: Counter = Metrics.counter("$metricsPrefix.response", "status", "success")
    private val responsFailure: Counter = Metrics.counter("$metricsPrefix.response", "status", "failure")

    inline fun <reified T : Any> getForEntity(
        uri: URI,
        httpHeaders: HttpHeaders? = null,
    ): T = executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.GET, HttpEntity(null, httpHeaders)) }

    inline fun <reified T : Any> postForEntity(
        uri: URI,
        payload: Any,
        httpHeaders: HttpHeaders? = null,
    ): T = executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.POST, HttpEntity(payload, httpHeaders)) }

    inline fun <reified T : Any> putForEntity(
        uri: URI,
        payload: Any,
        httpHeaders: HttpHeaders? = null,
    ): T = executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.PUT, HttpEntity(payload, httpHeaders)) }

    inline fun <reified T : Any> patchForEntity(
        uri: URI,
        payload: Any,
        httpHeaders: HttpHeaders? = null,
    ): T = executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.PATCH, HttpEntity(payload, httpHeaders)) }

    inline fun <reified T : Any> deleteForEntity(
        uri: URI,
        payload: Any? = null,
        httpHeaders: HttpHeaders? = null,
    ): T = executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.DELETE, HttpEntity(payload, httpHeaders)) }

    private fun <T> validerOgPakkUt(
        response: ResponseEntity<T>,
        uri: URI,
    ): T {
        if (!response.statusCode.is2xxSuccessful) {
            secureLogger.info("Kall mot {} feilet: {}", uri.toString(), response.body?.toString())
            log.info("Kall mot {} feilet: {}", uri.toString(), response.statusCode.toString())
            throw HttpServerErrorException(response.statusCode, "", response.body?.toString()?.toByteArray(), Charsets.UTF_8)
        }
        return response.body as T
    }

    fun <T> executeMedMetrics(
        uri: URI,
        function: () -> ResponseEntity<T>,
    ): T {
        try {
            val startTime = System.nanoTime()
            val responseEntity = function.invoke()
            responstid.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            responsSuccess.increment()
            return validerOgPakkUt(responseEntity, uri)
        } catch (e: RestClientResponseException) {
            responsFailure.increment()
            secureLogger.warn("RestClientResponseException ved kall mot uri={}", uri.toString(), e)
            lesRessurs(e)?.let { throw RessursException(it, e) } ?: throw e
        } catch (e: HttpClientErrorException) {
            responsFailure.increment()
            secureLogger.warn("HttpClientErrorException ved kall mot uri={}", uri.toString(), e)
            lesRessurs(e)?.let { throw RessursException(it, e) } ?: throw e
        } catch (e: ResourceAccessException) {
            responsFailure.increment()
            secureLogger.warn("ResourceAccessException ved kall mot uri={}", uri.toString(), e)
            throw e
        } catch (e: Exception) {
            responsFailure.increment()
            secureLogger.warn("Feil ved kall mot uri={}", uri.toString(), e)
            throw RuntimeException("Feil ved kall mot uri=$uri", e)
        }
    }

    private fun lesRessurs(e: RestClientResponseException): Ressurs<Any>? =
        try {
            if (e.responseBodyAsString.contains("status")) {
                objectMapper.readValue<Ressurs<Any>>(e.responseBodyAsString)
            } else {
                null
            }
        } catch (ex: Exception) {
            null
        }

    override fun toString(): String = this::class.simpleName + " [operations=" + operations + "]"
}
