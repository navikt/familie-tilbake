package no.nav.tilbakekreving.beregning.modell

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksresultatDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.math.BigDecimal

class Beregningsresultat(
    val beregningsresultatsperioder: List<Beregningsresultatsperiode>,
    val vedtaksresultat: Vedtaksresultat,
) {
    val totaltTilbakekrevesUtenRenter = beregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløpUtenRenter }
    val totaltTilbakekrevesMedRenter = beregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløp }
    val totaltRentebeløp = beregningsresultatsperioder.sumOf { it.rentebeløp }
    private val totaltSkattetrekk = beregningsresultatsperioder.sumOf { it.skattebeløp }
    val totaltTilbakekrevesBeløpMedRenterUtenSkatt: BigDecimal = totaltTilbakekrevesMedRenter.subtract(totaltSkattetrekk)
    val totaltFeilutbetaltBeløp = beregningsresultatsperioder.sumOf { it.feilutbetaltBeløp }

    override fun toString(): String {
        val perioder = beregningsresultatsperioder.joinToString("\n")
        return "Resultat: ${vedtaksresultat}\n$perioder"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Beregningsresultat) return false

        return vedtaksresultat == other.vedtaksresultat && beregningsresultatsperioder == other.beregningsresultatsperioder
    }

    fun tilFrontendDto(): BeregningsresultatDto {
        val beregningsresultatsperioder = beregningsresultatsperioder.map { periode ->
            val vurdering = periode.vurdering!!.tilBeregningsresultatVurderingDto()
            val reduksjon = beregnReduksjon(periode, vurdering)

            BeregningsresultatsperiodeDto(
                fom = periode.periode.fom,
                tom = periode.periode.tom,
                feilutbetaltBeløp = periode.feilutbetaltBeløp.toInt(),
                vurdering = vurdering,
                beløpIBehold = periode.beløpIbehold?.toInt(),
                reduksjon = reduksjon,
                rentebeløp = periode.rentebeløp.toInt(),
                skattebeløp = periode.skattebeløp.toInt().unaryMinus(),
                tilbakekrevingsbeløp = periode.tilbakekrevingsbeløpEtterSkatt.toInt(),
            )
        }

        return BeregningsresultatDto(
            beregningsresultatsperioder = beregningsresultatsperioder,
            vedtaksresultat = vedtaksresultat.tilVedtaksresultatDto(),
            totalBeløpIBehold = beregningsresultatsperioder.sumOf { it.beløpIBehold ?: 0 },
            totalReduksjon = beregningsresultatsperioder.sumOf { it.reduksjon ?: 0 },
            totalRentebeløp = beregningsresultatsperioder.sumOf { it.rentebeløp },
            totalSkattebeløp = beregningsresultatsperioder.sumOf { it.skattebeløp },
            totalTilbakekrevingsbeløp = beregningsresultatsperioder.sumOf { it.tilbakekrevingsbeløp },
        )
    }

    private fun beregnReduksjon(
        periode: Beregningsresultatsperiode,
        vurdering: BeregningsresultatVurderingDto,
    ): Int? = when (vurdering) {
        BeregningsresultatVurderingDto.Forsett -> null
        else -> {
            val beløp = periode.beløpIbehold ?: periode.feilutbetaltBeløp
            beløp.subtract(periode.tilbakekrevingsbeløpUtenRenter).toInt().unaryMinus()
        }
    }
}

private fun Vurdering.tilBeregningsresultatVurderingDto(): BeregningsresultatVurderingDto {
    return when (this) {
        Aktsomhet.FORSETT -> BeregningsresultatVurderingDto.Forsett
        Aktsomhet.GROV_UAKTSOMHET -> BeregningsresultatVurderingDto.GrovUaktsomhet
        Aktsomhet.SIMPEL_UAKTSOMHET -> BeregningsresultatVurderingDto.Uaktsomhet
        AnnenVurdering.GOD_TRO -> BeregningsresultatVurderingDto.GodTro
        NivåAvForståelse.Type.BurdeForstått, NivåAvForståelse.Type.MåForstått -> BeregningsresultatVurderingDto.BurdeForstått
        NivåAvForståelse.Type.Forstod -> BeregningsresultatVurderingDto.Forstod
        AnnenVurdering.FORELDET -> BeregningsresultatVurderingDto.Foreldet
        else -> error("Det er enten IkkeVurdert eller ukjent vurdering type for $this")
    }
}

private fun Vedtaksresultat.tilVedtaksresultatDto(): VedtaksresultatDto {
    return when (this) {
        Vedtaksresultat.FULL_TILBAKEBETALING -> VedtaksresultatDto.FullTilbakebetaling
        Vedtaksresultat.DELVIS_TILBAKEBETALING -> VedtaksresultatDto.DelvisTilbakebetaling
        Vedtaksresultat.INGEN_TILBAKEBETALING -> VedtaksresultatDto.IngenTilbakebetaling
    }
}
