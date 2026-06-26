package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

import java.math.BigDecimal
import java.time.LocalDate

data class HentKravgrunnlagDetaljerResponseDto(
    val status: Int,
    val melding: String,
    val kravgrunnlag: KravgrunnlagDetaljerDto,
)

data class KravgrunnlagDetaljerDto(
    val kravgrunnlagId: Long,
    val vedtakId: Long,
    val kodeStatusKrav: String,
    val kodeFagomraade: String,
    val fagsystemId: String,
    val datoVedtakFagsystem: LocalDate,
    val vedtakIdOmgjort: Long,
    val gjelderId: String,
    val typeGjelder: String,
    val utbetalesTilId: String,
    val typeUtbetalesTilId: String,
    val kodeHjemmel: String,
    val renterBeregnes: Boolean,
    val enhetAnsvarlig: String,
    val enhetBosted: String,
    val enhetBehandl: String,
    val kontrollfelt: String,
    val saksbehandlerId: String,
    val referanse: String,
    val perioder: List<DetaljerPeriodeDto>,
    val datoTilleggsfrist: LocalDate? = null,
)

data class DetaljerPeriodeDto(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val belopSkattMnd: BigDecimal,
    val posteringer: List<DetaljerPosteringDto>,
)

data class DetaljerPosteringDto(
    val kodeKlasse: String,
    val typeKlasse: String,
    val belopOpprinneligUtbetalt: BigDecimal,
    val belopNy: BigDecimal,
    val belopTilbakekreves: BigDecimal,
    val belopUinnkrevd: BigDecimal,
    val skattProsent: BigDecimal,
    val kodeResultat: String,
    val kodeAarsak: String,
    val kodeSkyld: String,
)
