package no.nav.tilbakekreving.integrasjoner.oppdrag

import java.math.BigDecimal
import java.time.LocalDate

data class HentKravgrunnlagResponseDto(
    val status: Int,
    val melding: String,
    val kravgrunnlagListe: List<KravgrunnlagDto>,
)

data class KravgrunnlagDto(
    val kravgrunnlagId: Long,
    val kodeStatusKrav: String,
    val gjelderId: String,
    val typeGjelder: String,
    val utbetalesTilId: String,
    val typeUtbetalesTil: String,
    val kodeFagomraade: String,
    val fagsystemId: String,
    val datoVedtakFagsystem: LocalDate,
    val enhetBosted: String,
    val enhetAnsvarlig: String,
    val datoKravDannet: LocalDate,
    val datoPeriodeFom: LocalDate,
    val datoPeriodeTom: LocalDate,
    val belopSumFeilutbetalt: BigDecimal,
)
