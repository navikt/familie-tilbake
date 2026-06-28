package no.nav.tilbakekreving.integrasjoner.oppdrag

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.HentKravgrunnlagDetaljerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagAnnulerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakResponseDto
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.math.BigInteger

interface OppdragRestClient {
    fun iverksettVedtak(request: TilbakekrevingsvedtakRequestDto): TilbakekrevingsvedtakResponseDto

    fun hentKravgrunnlag(
        kravgrunnlagId: BigInteger,
        kodeAksjon: String,
    ): HentKravgrunnlagDetaljerResponseDto

    fun annullerKravgrunnlag(vedtakId: BigInteger): KravgrunnlagAnnulerResponseDto

    companion object {
        fun opprett(
            config: Config,
            tokenExchangeService: TokenExchangeService,
        ): OppdragRestClient {
            return OppdragRestClientImpl(
                config = config,
                httpClient = HttpClient(Apache5) {
                    install(ContentNegotiation) {
                        jackson {
                            registerModule(JavaTimeModule())
                            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        }
                    }
                },
                tokenExchangeService = tokenExchangeService,
            )
        }

        data class Config(
            val baseUrl: String,
            val scope: String,
        )
    }
}
