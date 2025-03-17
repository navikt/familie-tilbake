package no.nav.tilbakekreving.api.v1.dto

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class FaktaFeilutbetalingDto(
    val varsletBeløp: Long? = null,
    val totalFeilutbetaltPeriode: Datoperiode,
    val feilutbetaltePerioder: List<FeilutbetalingsperiodeDto>,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val revurderingsvedtaksdato: LocalDate,
    val begrunnelse: String,
    val faktainfo: Faktainfo,
    val kravgrunnlagReferanse: String,
    val vurderingAvBrukersUttalelse: VurderingAvBrukersUttalelseDto,
    val opprettetTid: LocalDateTime? = null,
) {
    val gjelderDødsfall get() = feilutbetaltePerioder.any { it.hendelsestype == Hendelsestype.DØDSFALL }
}

data class FeilutbetalingsperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val hendelsestype: Hendelsestype? = null,
    val hendelsesundertype: Hendelsesundertype? = null,
)

data class VurderingAvBrukersUttalelseDto(
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
    val beskrivelse: String?,
) {
    companion object {
        fun ikkeVurdert(): VurderingAvBrukersUttalelseDto =
            VurderingAvBrukersUttalelseDto(
                harBrukerUttaltSeg = HarBrukerUttaltSeg.IKKE_VURDERT,
                beskrivelse = null
            )
    }
}
