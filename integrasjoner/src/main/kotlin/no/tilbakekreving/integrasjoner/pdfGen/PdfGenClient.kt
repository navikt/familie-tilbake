package no.tilbakekreving.integrasjoner.pdfGen

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto

interface PdfGenClient {
    fun hentPdfForVedtak(vedtaksbrevData: VedtaksbrevDataDto): ByteArray

    companion object {
        fun opprett(
            config: Config,
        ): PdfGenClient {
            return PdfGenClientImpl(
                config = config,
                httpClient = HttpClient(Apache) {
                    install(ContentNegotiation) {
                        jackson {
                            registerModule(JavaTimeModule())
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        }
                    }
                },
            )
        }

        data class Config(
            val baseUrl: String,
        )
    }
}
