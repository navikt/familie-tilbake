package no.nav.tilbakekreving.integrasjoner.oppdrag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.HentKravgrunnlagDetaljerRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.HentKravgrunnlagDetaljerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagAnnulerRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagAnnulerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakResponseDto
import no.nav.tilbakekreving.integrasjoner.tokenexchange.TokenExchangeService
import java.math.BigInteger

internal class OppdragRestClientImpl(
    private val config: OppdragRestClient.Companion.Config,
    private val httpClient: HttpClient,
    private val tokenExchangeService: TokenExchangeService,
) : OppdragRestClient {
    override fun iverksettVedtak(request: TilbakekrevingsvedtakRequestDto): TilbakekrevingsvedtakResponseDto {
        return runBlocking {
            val token = tokenExchangeService.clientCredentialsToken(config.scope)
            httpClient.post(
                buildUrl {
                    takeFrom(config.baseUrl)
                    appendPathSegments("api", "v1", "tilbakekreving", "vedtak")
                },
            ) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(request)
            }.body()
        }
    }

    override fun hentKravgrunnlag(kravgrunnlagId: BigInteger, kodeAksjon: String): HentKravgrunnlagDetaljerResponseDto {
        return runBlocking {
            val token = tokenExchangeService.clientCredentialsToken(config.scope)
            httpClient.post(
                buildUrl {
                    takeFrom(config.baseUrl)
                    appendPathSegments("api", "v1", "tilbakekreving", "kravgrunnlag", "detaljer")
                },
            ) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    HentKravgrunnlagDetaljerRequestDto(
                        kodeAksjon = kodeAksjon,
                        kravgrunnlagId = kravgrunnlagId.toInt(),
                        enhetAnsvarlig = "8020", // fast verdi
                        saksbehandlerId = "K231B433", // fast verdi
                    ),
                )
            }.body()
        }
    }

    override fun annullerKravgrunnlag(vedtakId: BigInteger): KravgrunnlagAnnulerResponseDto {
        return runBlocking {
            val token = tokenExchangeService.clientCredentialsToken(config.scope)
            httpClient.post(
                buildUrl {
                    takeFrom(config.baseUrl)
                    appendPathSegments("api", "v1", "tilbakekreving", "kravgrunnlag", "annuller")
                },
            ) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(
                    KravgrunnlagAnnulerRequestDto(
                        kodeAksjon = KodeAksjonDto.ANNULLERT_KRAVGRUNNLAG,
                        vedtakId = vedtakId.toInt(),
                        enhetAnsvarlig = "8020", // fast verdi
                        saksbehandlerId = "K231B433", // fast verdi
                    ),
                )
            }.body()
        }
    }
}
