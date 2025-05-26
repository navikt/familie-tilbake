package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert.Utbetalingsperiode.Companion.fordelRentebeløp
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert.Utbetalingsperiode.Companion.fordelSkattebeløp
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert.Utbetalingsperiode.Companion.fordelTilbakekrevingsbeløp
import no.nav.tilbakekreving.beregning.delperiode.Vurderingsperiode
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.RoundingMode

class Beregning(
    beregnRenter: Boolean,
    private val tilbakekrevLavtBeløp: Boolean,
    vilkårsvurdering: VilkårsvurderingAdapter,
    foreldetPerioder: List<Datoperiode>,
    kravgrunnlag: KravgrunnlagAdapter,
) {
    init {
        kravgrunnlag.perioder().forEach { kravgrunnlagsperiode ->
            val vurdertePerioder = foreldetPerioder + vilkårsvurdering.perioder().map(VilkårsvurdertPeriodeAdapter::periode)
            require(vurdertePerioder.any { kravgrunnlagsperiode.periode() in it }) {
                "Perioden ${kravgrunnlagsperiode.periode()} mangler vilkårsvurdering eller foreldelse"
            }
        }
    }

    private val foreldetUtbetalinger = foreldetPerioder
        .sortedBy { it.fom }
        .map { foreldetPeriode ->
            val relevanteKravgrunnlag = kravgrunnlag.perioder()
                .filter { it.periode() in foreldetPeriode }

            Foreldet.opprett(foreldetPeriode, relevanteKravgrunnlag)
        }
    private val vilkårsvurderteUtbetalinger = vilkårsvurdering.perioder()
        .sortedBy { it.periode().fom }
        .map { vurdering ->
            val relevanteKravgrunnlag = kravgrunnlag.perioder()
                .filter { it.periode() in vurdering.periode() }

            Vilkårsvurdert(vurdering, beregnRenter, relevanteKravgrunnlag)
        }

    private val allePerioder get() = (foreldetUtbetalinger + vilkårsvurderteUtbetalinger).sortedBy { it.periode.fom }

    fun beregn(): List<Delperiode> {
        vilkårsvurderteUtbetalinger.flatMap { it.delperioder }
            .fordelTilbakekrevingsbeløp()
            .fordelRentebeløp()
            .fordelSkattebeløp()

        return allePerioder.flatMap { it.delperioder }
    }

    fun oppsummer(): Beregningsresultat {
        val delperioder = beregn()
        return Beregningsresultat(
            vedtaksresultat = bestemVedtaksresultat(delperioder),
            beregningsresultatsperioder = allePerioder.map(Vurderingsperiode<*>::beregningsresultat),
        )
    }

    fun vedtaksresultat(): Vedtaksresultat = bestemVedtaksresultat(beregn())

    private fun bestemVedtaksresultat(delperioder: List<Delperiode>): Vedtaksresultat {
        val tilbakekrevingsbeløp = delperioder.sumOf { it.tilbakekrevesBruttoMedRenter() }.setScale(0, RoundingMode.HALF_UP)
        val feilutbetaltBeløp = delperioder.sumOf { it.feilutbetaltBeløp() }.setScale(0, RoundingMode.HALF_UP)
        return when {
            !tilbakekrevLavtBeløp -> Vedtaksresultat.INGEN_TILBAKEBETALING
            tilbakekrevingsbeløp.isZero() -> Vedtaksresultat.INGEN_TILBAKEBETALING
            tilbakekrevingsbeløp < feilutbetaltBeløp -> Vedtaksresultat.DELVIS_TILBAKEBETALING
            else -> return Vedtaksresultat.FULL_TILBAKEBETALING
        }
    }
}
