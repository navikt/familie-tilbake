package no.nav.tilbakekreving.beregning

import no.nav.tilbakekreving.beregning.adapter.KravgrunnlagAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.math.RoundingMode
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.delperiode.Vilkårsvurdert
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode

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

    fun beregn(): Beregningsresultat {
        val beregnignsresultater = fordelt.map(Delperiode::beregningsresultat)
            .fordelTilbakekrevingsbeløp()
            .fordelRenter()
            .fordelSkatt()
        return Beregningsresultat(
            vedtaksresultat = bestemVedtakResultat(
                tilbakekrevingsbeløp = beregnignsresultater.sumOf { it.tilbakekrevingsbeløp }.setScale(0, RoundingMode.HALF_UP),
                feilutbetaltBeløp = beregnignsresultater.sumOf { it.feilutbetaltBeløp }.setScale(0, RoundingMode.HALF_UP)
            ),
            beregningsresultatsperioder = beregnignsresultater,
        )
    }

    private fun List<Beregningsresultatsperiode>.fordelTilbakekrevingsbeløp(): List<Beregningsresultatsperiode> {
        val diff = sumOf { it.tilbakekrevingsbeløpUtenRenter - it.tilbakekrevingsbeløpUtenRenter.setScale(0, RoundingMode.DOWN) }
            .setScale(0, RoundingMode.HALF_DOWN)
            .toInt()
        val biggestDiffs = sortedByDescending { it.tilbakekrevingsbeløpUtenRenter - it.tilbakekrevingsbeløpUtenRenter.setScale(0, RoundingMode.DOWN) }
        return (biggestDiffs.take(diff).map {
            it.copy(
                tilbakekrevingsbeløpUtenRenter = it.tilbakekrevingsbeløpUtenRenter.setScale(0, RoundingMode.DOWN) + BigDecimal.ONE,
            )
        } + biggestDiffs.drop(diff).map {
            it.copy(
                tilbakekrevingsbeløpUtenRenter = it.tilbakekrevingsbeløpUtenRenter.setScale(0, RoundingMode.DOWN),
            )
        }).sortedBy { it.periode.fom }
    }

    private fun List<Beregningsresultatsperiode>.fordelRenter(): List<Beregningsresultatsperiode> {
        val diff = sumOf { it.rentebeløp - it.rentebeløp.setScale(0, RoundingMode.DOWN) }
            .setScale(0, RoundingMode.DOWN)
            .toInt()
        val biggestDiffs = sortedByDescending { it.rentebeløp - it.rentebeløp.setScale(0, RoundingMode.DOWN) }
        return (biggestDiffs.take(diff).map {
            it.copy(
                rentebeløp = it.rentebeløp.setScale(0, RoundingMode.DOWN) + BigDecimal.ONE,
            )
        } + biggestDiffs.drop(diff).map {
            it.copy(
                rentebeløp = it.rentebeløp.setScale(0, RoundingMode.DOWN),
            )
        }).sortedBy { it.periode.fom }
    }

    private fun List<Beregningsresultatsperiode>.fordelSkatt(): List<Beregningsresultatsperiode> {
        val diff = sumOf { it.skattebeløp - it.skattebeløp.setScale(0, RoundingMode.DOWN) }
            .setScale(0, RoundingMode.DOWN)
            .toInt()
        val biggestDiffs = sortedByDescending { it.skattebeløp - it.skattebeløp.setScale(0, RoundingMode.DOWN) }
        return (biggestDiffs.take(diff).map {
            val skattebeløp = it.skattebeløp.setScale(0, RoundingMode.DOWN) + BigDecimal.ONE
            val tilbakekrevingsbeløp = it.tilbakekrevingsbeløpUtenRenter + it.rentebeløp
            it.copy(
                tilbakekrevingsbeløpEtterSkatt = tilbakekrevingsbeløp - skattebeløp,
                tilbakekrevingsbeløp = tilbakekrevingsbeløp,
                feilutbetaltBeløp = it.feilutbetaltBeløp.setScale(0, RoundingMode.HALF_DOWN),
                manueltSattTilbakekrevingsbeløp = it.manueltSattTilbakekrevingsbeløp?.setScale(2, RoundingMode.HALF_DOWN),
                skattebeløp = skattebeløp,
                utbetaltYtelsesbeløp = it.utbetaltYtelsesbeløp.setScale(0, RoundingMode.HALF_DOWN),
                riktigYtelsesbeløp = it.riktigYtelsesbeløp.setScale(0, RoundingMode.HALF_DOWN),
            )
        } + biggestDiffs.drop(diff).map {
            val skattebeløp = it.skattebeløp.setScale(0, RoundingMode.DOWN)
            val tilbakekrevingsbeløp = it.tilbakekrevingsbeløpUtenRenter + it.rentebeløp
            it.copy(
                tilbakekrevingsbeløpEtterSkatt = tilbakekrevingsbeløp - skattebeløp,
                tilbakekrevingsbeløp = tilbakekrevingsbeløp,
                feilutbetaltBeløp = it.feilutbetaltBeløp.setScale(0, RoundingMode.HALF_DOWN),
                manueltSattTilbakekrevingsbeløp = it.manueltSattTilbakekrevingsbeløp?.setScale(2, RoundingMode.HALF_DOWN),
                skattebeløp = skattebeløp,
                utbetaltYtelsesbeløp = it.utbetaltYtelsesbeløp.setScale(0, RoundingMode.HALF_DOWN),
                riktigYtelsesbeløp = it.riktigYtelsesbeløp.setScale(0, RoundingMode.HALF_DOWN),
            )
        }).sortedBy { it.periode.fom }
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
