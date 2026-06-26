package no.nav.familie.tilbake.config

import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakResponseDto
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.concurrent.ConcurrentLinkedQueue

@Primary
@Service
class OppdragClientRestMock : OppdragRestClient {
    private val iverksettelseRequests = ConcurrentLinkedQueue<TilbakekrevingsvedtakRequestDto>()

    override fun iverksettVedtak(request: TilbakekrevingsvedtakRequestDto): TilbakekrevingsvedtakResponseDto {
        iverksettelseRequests.add(request)
        return TilbakekrevingsvedtakResponseDto(
            status = 0,
            melding = "OK",
            vedtakId = request.vedtakId,
            datoVedtakFagsystem = request.vedtaksDato,
        )
    }

    fun shouldHaveIverksettelse(
        vedtakId: BigInteger,
        callback: (vedtak: TilbakekrevingsvedtakRequestDto) -> Unit = {},
    ) {
        iverksettelseRequests.forOne { it.vedtakId shouldBe vedtakId }
        callback(iverksettelseRequests.single { it.vedtakId == vedtakId })
    }
}
