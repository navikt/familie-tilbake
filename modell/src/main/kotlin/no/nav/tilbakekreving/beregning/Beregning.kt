package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
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
        kravgrunnlag.perioder().forEach { periode ->
            require((foreldetPerioder + vilkårsvurdering.perioder().map { it.periode() }).any { periode.periode() in it }) {
                "Perioden ${periode.periode()} mangler vilkårsvurdering eller foreldelse"
            }
        }
    }

    private val fordelt: List<Delperiode> = foreldetPerioder.flatMap { foreldetPeriode ->
        kravgrunnlag.perioder().filter { it.periode() in foreldetPeriode }
            .map { Foreldet.opprett(it.periode(), it) }
    } + vilkårsvurdering.perioder().flatMap { vurdering ->
        val kgPerioder = kravgrunnlag.perioder()
            .filter { it.periode() in vurdering.periode() }

        kgPerioder.map { Vilkårsvurdert.opprett(vurdering, it, beregnRenter, kgPerioder.size) }
    }

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
            vedtaksresultat = bestemVedtakResultat(delperioder),
            beregningsresultatsperioder = beregningsresultater,
        )
    }

    fun vedtaksresultat(): Vedtaksresultat = bestemVedtakResultat(beregn())

    private fun bestemVedtakResultat(delperioder: List<Delperiode>): Vedtaksresultat {
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
