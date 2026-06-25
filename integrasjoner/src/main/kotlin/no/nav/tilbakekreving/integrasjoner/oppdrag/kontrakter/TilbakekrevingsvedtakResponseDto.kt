package no.nav.tilbakekreving.integrasjoner.oppdrag

import java.math.BigInteger
import java.time.LocalDate

data class TilbakekrevingsvedtakResponseDto(
    val status: Int,
    val melding: String,
    val vedtakId: BigInteger,
    val datoVedtakFagsystem: LocalDate,
)
