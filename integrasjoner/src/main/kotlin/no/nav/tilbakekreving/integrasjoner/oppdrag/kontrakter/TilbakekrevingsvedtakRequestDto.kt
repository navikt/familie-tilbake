package no.nav.tilbakekreving.integrasjoner.oppdrag

import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

data class TilbakekrevingsvedtakRequestDto(
    val kodeAksjon: KodeAksjonDto,
    val vedtakId: BigInteger,
    val vedtaksDato: LocalDate,
    val kodeHjemmel: String,
    val renterBeregnes: Boolean,
    val enhetAnsvarlig: String,
    val kontrollfelt: String,
    val saksbehandlerId: String,
    val perioder: List<VedtakPeriodeDto>,
    val datoTilleggsfrist: LocalDate?,
)

data class VedtakPeriodeDto(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val renterPeriodeBeregnes: Boolean,
    val belopRenter: BigDecimal,
    val posteringer: List<PosteringDto>,
)

data class PosteringDto(
    val kodeKlasse: String,
    val belopOpprinneligUtbetalt: BigDecimal,
    val belopNy: BigDecimal,
    val belopTilbakekreves: BigDecimal,
    val belopUinnkrevd: BigDecimal,
    val belopSkatt: BigDecimal,
    val kodeResultat: String,
    val kodeAarsak: String,
    val kodeSkyld: String,
)
