package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

class Beregning(
    beregnRenter: Boolean,
    private val tilbakekrevLavtBeløp: Boolean,
    vilkårsvurdering: VilkårsvurderingAdapter,
    foreldetPerioder: List<Datoperiode>,
    kravgrunnlag: KravgrunnlagAdapter,
) {
    private val fordeltKravgrunnlag = KravgrunnlagsberegningUtil.fordelKravgrunnlagBeløpPåPerioder(
        kravgrunnlag = kravgrunnlag,
        vurderingsperioder = foreldetPerioder + vilkårsvurdering.perioder().map(VilkårsvurdertPeriodeAdapter::periode),
    )
    private val foreldelseBeregning = ForeldelseBeregning(fordeltKravgrunnlag, foreldetPerioder)

    private val vilkårsvurderingBeregning = VilkårsvurderingBeregning(
        kravgrunnlag = kravgrunnlag,
        vilkårsvurdering = vilkårsvurdering,
        kravbeløpPerPeriode = fordeltKravgrunnlag,
        beregnRenter = beregnRenter,
    )

    fun beregn(): Beregningsresultat {
        val beregningsresultatperioder = (foreldelseBeregning.beregn() + vilkårsvurderingBeregning.beregn())
            .sortedBy { it.periode.fom }

        return Beregningsresultat(
            vedtaksresultat = bestemVedtakResultat(
                tilbakekrevingsbeløp = beregningsresultatperioder.sumOf { it.tilbakekrevingsbeløp },
                feilutbetaltBeløp = beregningsresultatperioder.sumOf { it.feilutbetaltBeløp },
            ),
            beregningsresultatsperioder = beregningsresultatperioder,
        )
    }

    private fun bestemVedtakResultat(
        tilbakekrevingsbeløp: BigDecimal,
        feilutbetaltBeløp: BigDecimal?,
    ): Vedtaksresultat {
        return when {
            tilbakekrevLavtBeløp -> Vedtaksresultat.INGEN_TILBAKEBETALING
            tilbakekrevingsbeløp.isZero() -> Vedtaksresultat.INGEN_TILBAKEBETALING
            tilbakekrevingsbeløp < feilutbetaltBeløp -> Vedtaksresultat.DELVIS_TILBAKEBETALING
            else -> return Vedtaksresultat.FULL_TILBAKEBETALING
        }
    }
}
