package no.tilbakekreving.integrasjoner.dokument.saf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.dokument.kontrakter.Bruker
import no.tilbakekreving.integrasjoner.dokument.kontrakter.IntegrasjonTema
import no.tilbakekreving.integrasjoner.dokument.kontrakter.JournalpostResponse
import no.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.util.UUID

interface SafClient {
    fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
        callContext: CallContext.Saksbehandler,
    ): ByteArray

    fun hentJournalposterForBruker(
        bruker: Bruker,
        tema: List<IntegrasjonTema>,
        graphqlQuery: String,
    ): List<JournalpostResponse>

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): SafClient {
            return SafClientImpl(
                config = config,
                tokenExchangeService = tokenExchangeService,
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
            val scope: String,
        )
    }
}
