package no.nav.familie.tilbake.leader

import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

object LeaderClient {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /**
     * @return Om pod er leader eller ikke. null hvis leadersjekk ikke er implementert på pod
     */
    @JvmStatic
    fun isLeader(
        antallGanger: Int = 3,
        forsinkelseIms: Long = 1000,
    ): Boolean? {
        val electorGetUrl = Environment.hentLeaderSystemEnv() ?: return null

        val client = HttpClient.newHttpClient()
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(electorGetUrl))
                .GET()
                .build()

        val response: HttpResponse<String>? =
            retryFunksjon(
                antallGanger = antallGanger,
                forsinkelseIms = forsinkelseIms,
            ) { client.send(request, BodyHandlers.ofString()) }

        if (response?.body().isNullOrBlank()) return null

        val hostname: String = InetAddress.getLocalHost().hostName
        return response?.body()?.contains(hostname)
    }

    private fun <T> retryFunksjon(
        antallGanger: Int = 3,
        forsinkelseIms: Long = 1000,
        funksjon: () -> T,
    ): T? {
        var throwable: Exception? = null
        repeat(antallGanger) {
            try {
                return funksjon()
            } catch (e: Exception) {
                secureLogger.warn("Kunne ikke hente leader status")
                throwable = e
            }
            Thread.sleep(forsinkelseIms)
        }
        secureLogger.error("Kunne ikke hente leader status", throwable)
        return null
    }
}
