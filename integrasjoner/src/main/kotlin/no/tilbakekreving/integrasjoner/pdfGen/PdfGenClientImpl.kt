package no.tilbakekreving.integrasjoner.pdfGen

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto

class PdfGenClientImpl(
    private val config: PdfGenClient.Companion.Config,
    private val httpClient: HttpClient,
) : PdfGenClient {
    override fun hentPdfForVedtak(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
        return runBlocking {
            hentPdf(vedtaksbrevData)
        }
    }

    private suspend fun hentPdf(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
        val baseUrl = config.baseUrl

        val response = httpClient.post("$baseUrl/api/v1/brev/vedtaksbrev/pdf") {
            contentType(ContentType.Application.Json)
            setBody(vedtaksbrevData)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Feil fra PDF-service: ${response.status}")
        }
        return response.body()
    }
}
