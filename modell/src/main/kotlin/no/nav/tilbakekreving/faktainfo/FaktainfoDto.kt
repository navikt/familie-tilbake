package no.nav.tilbakekreving.faktainfo

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.midlertidig.FaktafeilutbetalingSuperDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class FaktainfoDto(
    val varsletBeløp: Long? = null,
    val totalFeilutbetaltPeriode: Datoperiode,
    val feilutbetaltePerioder: List<FeilutbetalingsperiodeDto>,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val revurderingsvedtaksdato: LocalDate,
    val begrunnelse: String?,
    val faktainfo: Faktainfo,
    val kravgrunnlagReferanse: String,
    val vurderingAvBrukersUttalelse: HarBrukerUttaltSeg? = null,
    val opprettetTid: LocalDateTime? = null,
) : FaktafeilutbetalingSuperDto() {
    val gjelderDødsfall get() = feilutbetaltePerioder.any { it.hendelsestype == Hendelsestype.DØDSFALL }
}

data class FeilutbetalingsperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val hendelsestype: Hendelsestype? = null,
    val hendelsesundertype: Hendelsesundertype? = null,
)
