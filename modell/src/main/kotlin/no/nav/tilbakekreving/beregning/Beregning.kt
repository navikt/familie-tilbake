package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Delperiode.Companion.oppsummer
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert.Companion.fordelRentebeløp
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert.Companion.fordelSkattebeløp
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert.Companion.fordelTilbakekrevingsbeløp
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

    private val foreldet = foreldetPerioder.flatMap { foreldetPeriode ->
        kravgrunnlag.perioder()
            .filter { it.periode() in foreldetPeriode }
            .map { Foreldet.opprett(foreldetPeriode, it) }
    }
    private val vilkårsvurdert = vilkårsvurdering.perioder().flatMap { vurdering ->
        val kgPerioder = kravgrunnlag.perioder()
            .filter { it.periode() in vurdering.periode() }

        kgPerioder.map { Vilkårsvurdert.opprett(vurdering, it, beregnRenter, kgPerioder.size) }
    }

    private val fordelt: List<Delperiode> = (foreldet + vilkårsvurdert).sortedBy { it.periode.fom }

    fun beregn(): List<Delperiode> {
        return fordelt
            .fordelTilbakekrevingsbeløp()
            .fordelRentebeløp()
            .fordelSkattebeløp()
    }

    fun oppsummer(): Beregningsresultat {
        val delperioder = beregn()
        val beregningsresultater = delperioder.oppsummer()
        return Beregningsresultat(
            vedtaksresultat = bestemVedtaksresultat(delperioder),
            beregningsresultatsperioder = beregningsresultater,
        )
    }

    fun vedtaksresultat(): Vedtaksresultat = bestemVedtaksresultat(beregn())

    private fun bestemVedtaksresultat(delperioder: List<Delperiode>): Vedtaksresultat {
        val tilbakekrevingsbeløp = delperioder.sumOf { it.tilbakekrevesBruttoMedRenter() }.setScale(0, RoundingMode.HALF_UP)
        val feilutbetaltBeløp = delperioder.sumOf { it.andel.feilutbetaltBeløp() }.setScale(0, RoundingMode.HALF_UP)
        return when {
            tilbakekrevLavtBeløp -> Vedtaksresultat.INGEN_TILBAKEBETALING
            tilbakekrevingsbeløp.isZero() -> Vedtaksresultat.INGEN_TILBAKEBETALING
            tilbakekrevingsbeløp < feilutbetaltBeløp -> Vedtaksresultat.DELVIS_TILBAKEBETALING
            else -> return Vedtaksresultat.FULL_TILBAKEBETALING
        }
    }
}
