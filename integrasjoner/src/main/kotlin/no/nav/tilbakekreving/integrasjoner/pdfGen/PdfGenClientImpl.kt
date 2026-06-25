package no.nav.tilbakekreving.integrasjoner.pdfGen

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.kontrakter.frontend.models.VarselbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto

class PdfGenClientImpl(
    private val config: PdfGenClient.Companion.Config,
    private val httpClient: HttpClient,
) : PdfGenClient {
    override fun hentPdfForVedtak(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
        return runBlocking {
            hentVedtaksbrevPdf(vedtaksbrevData)
        }
    }

    override fun hentPdfForForhåndsvarsel(varselbrevData: VarselbrevDataDto): ByteArray {
        return runBlocking {
            hentVarselbrevPdf(varselbrevData)
        }
    }

    private suspend fun hentVedtaksbrevPdf(vedtaksbrevData: VedtaksbrevDataDto): ByteArray {
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

    private suspend fun hentVarselbrevPdf(varselbrevData: VarselbrevDataDto): ByteArray {
        val baseUrl = config.baseUrl

        val response = httpClient.post("$baseUrl/api/v1/brev/forhåndsvarsel/pdf") {
            contentType(ContentType.Application.Json)
            setBody(varselbrevData)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Feil fra PDF-service: ${response.status}")
        }
        return response.body()
    }
}
