package no.nav.tilbakekreving.beregning.modell

import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksresultatDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
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
        return BeregningsresultatDto(
            beregningsresultatsperioder = beregningsresultatsperioder.mapNotNull { periode ->
                val vurdering = periode.vurdering?.tilBeregningsresultatVurderingDto() ?: return@mapNotNull null
                BeregningsresultatsperiodeDto(
                    fom = periode.periode.fom,
                    tom = periode.periode.tom,
                    feilutbetaltBeløp = periode.feilutbetaltBeløp.toInt(),
                    vurdering = vurdering,
                    andelAvBeløp = periode.andelAvBeløp?.toInt(),
                    renteprosent = periode.renteprosent?.toInt(),
                    tilbakekrevingsbeløp = periode.tilbakekrevingsbeløp.toInt(),
                    tilbakekrevesBeløpEtterSkatt = periode.tilbakekrevingsbeløpEtterSkatt.toInt(),
                )
            },
            vedtaksresultat = vedtaksresultat.tilVedtaksresultatDto(),
        )
    }
}

private fun no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering.tilBeregningsresultatVurderingDto(): BeregningsresultatVurderingDto? {
    return when (this) {
        Aktsomhet.FORSETT -> BeregningsresultatVurderingDto.Forsett
        Aktsomhet.GROV_UAKTSOMHET -> BeregningsresultatVurderingDto.GrovUaktsomhet
        Aktsomhet.SIMPEL_UAKTSOMHET -> BeregningsresultatVurderingDto.SimpelUaktsomhet
        AnnenVurdering.GOD_TRO -> BeregningsresultatVurderingDto.GodTro
        else -> null
    }
}

private fun Vedtaksresultat.tilVedtaksresultatDto(): VedtaksresultatDto {
    return when (this) {
        Vedtaksresultat.FULL_TILBAKEBETALING -> VedtaksresultatDto.FullTilbakebetaling
        Vedtaksresultat.DELVIS_TILBAKEBETALING -> VedtaksresultatDto.DelvisTilbakebetaling
        Vedtaksresultat.INGEN_TILBAKEBETALING -> VedtaksresultatDto.IngenTilbakebetaling
    }
}
