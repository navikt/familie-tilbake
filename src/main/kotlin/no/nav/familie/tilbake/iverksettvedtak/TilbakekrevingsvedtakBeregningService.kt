package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.isGreaterThanZero
import no.nav.familie.tilbake.common.isLessThanZero
import no.nav.familie.tilbake.common.isZero
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class TilbakekrevingsvedtakBeregningService(private val tilbakekrevingsberegningService: TilbakekrevingsberegningService) {

    fun beregnVedtaksperioder(behandlingId: UUID,
                              kravgrunnlag431: Kravgrunnlag431): List<Tilbakekrevingsperiode> {
        val beregningsresultat = tilbakekrevingsberegningService.beregn(behandlingId)

        val kravgrunnlagsperioder = kravgrunnlag431.perioder.toList().sortedBy { it.periode.fom }
        val beregnetPeioder = beregningsresultat.beregningsresultatsperioder.sortedBy { it.periode.fom }

        // oppretter kravgrunnlagsperioderMedSkatt basert på månedligSkattebeløp
        var kravgrunnlagsperioderMedSkatt = kravgrunnlagsperioder.associate { it.periode to it.månedligSkattebeløp }

        val tilbakekrevingsperioder = mutableListOf<Tilbakekrevingsperiode>()
        for (beregnetPeriode in beregnetPeioder) {
            var perioder = lagTilbakekrevingsperioder(kravgrunnlagsperioder, beregnetPeriode)

            // avrunding tilbakekrevesbeløp og uinnkrevd beløp
            perioder = justerAvrunding(beregnetPeriode, perioder)

            // skatt
            // oppdaterer kravgrunnlagsperioderMedSkatt med gjenstående skatt
            // (ved å trekke totalSkattBeløp fra månedligeSkattebeløp)
            kravgrunnlagsperioderMedSkatt = oppdaterGjenståendeSkattetrekk(perioder, kravgrunnlagsperioderMedSkatt)
            perioder = justerAvrundingSkatt(beregnetPeriode, perioder, kravgrunnlagsperioderMedSkatt.toMutableMap())

            //renter
            val totalTilbakekrevingsbeløp = beregnTotalTilbakekrevesbeløp(perioder)
            var renteBeløp = BigDecimal.ZERO
            if (beregnetPeriode.tilbakekrevingsbeløpUtenRenter != BigDecimal.ZERO) {
                renteBeløp = beregnetPeriode.rentebeløp.multiply(totalTilbakekrevingsbeløp)
                        .divide(beregnetPeriode.tilbakekrevingsbeløpUtenRenter, 0, RoundingMode.HALF_UP)
            }
            perioder.forEach { it.renter = renteBeløp }

            tilbakekrevingsperioder.addAll(perioder)
        }
        return tilbakekrevingsperioder
    }

    private fun lagTilbakekrevingsperioder(kravgrunnlagsperioder: List<Kravgrunnlagsperiode432>,
                                           beregnetPeriode: Beregningsresultatsperiode): List<Tilbakekrevingsperiode> {
        return kravgrunnlagsperioder.filter { it.periode.snitt(beregnetPeriode.periode) != null }
                .map { Tilbakekrevingsperiode(it.periode, BigDecimal.ZERO, lagTilbakekrevingsbeløp(it.beløp, beregnetPeriode)) }
    }

    private fun lagTilbakekrevingsbeløp(kravgrunnlagsbeløp: Set<Kravgrunnlagsbeløp433>,
                                        beregnetPeriode: Beregningsresultatsperiode): List<Tilbakekrevingsbeløp> {
        val antallMåned = BigDecimal(beregnetPeriode.periode.lengdeIMåneder())
        val tilbakrekrevesbeløp = beregnetPeriode.tilbakekrevingsbeløp.divide(antallMåned, 0, RoundingMode.HALF_UP)

        return kravgrunnlagsbeløp.mapNotNull {
            when (it.klassetype) {
                Klassetype.FEIL -> Tilbakekrevingsbeløp(klassetype = it.klassetype,
                                                        klassekode = it.klassekode,
                                                        nyttBeløp = it.nyttBeløp,
                                                        utbetaltBeløp = BigDecimal.ZERO,
                                                        tilbakekrevesBeløp = BigDecimal.ZERO,
                                                        uinnkrevdBeløp = BigDecimal.ZERO,
                                                        skattBeløp = BigDecimal.ZERO,
                                                        kodeResultat = utledKodeResulat(beregnetPeriode))
                Klassetype.YTEL -> Tilbakekrevingsbeløp(klassetype = it.klassetype,
                                                        klassekode = it.klassekode,
                                                        nyttBeløp = it.nyttBeløp,
                                                        utbetaltBeløp = it.opprinneligUtbetalingsbeløp,
                                                        tilbakekrevesBeløp = tilbakrekrevesbeløp,
                                                        uinnkrevdBeløp = it.opprinneligUtbetalingsbeløp
                                                                .subtract(tilbakrekrevesbeløp),
                                                        skattBeløp = beregnSkattBeløp(tilbakrekrevesbeløp,
                                                                                      it.skatteprosent),
                                                        kodeResultat = utledKodeResulat(beregnetPeriode))

                else -> null
            }
        }
    }

    private fun beregnSkattBeløp(bruttoTilbakekrevesBeløp: BigDecimal,
                                 skattProsent: BigDecimal): BigDecimal {
        return bruttoTilbakekrevesBeløp.multiply(skattProsent).divide(BigDecimal(100), 0, RoundingMode.DOWN)
    }

    private fun utledKodeResulat(beregnetPeriode: Beregningsresultatsperiode): KodeResultat {
        return when {
            beregnetPeriode.vurdering?.let { it == AnnenVurdering.FORELDET } == true -> {
                KodeResultat.FORELDET
            }
            beregnetPeriode.tilbakekrevingsbeløpUtenRenter.isZero() -> {
                KodeResultat.INGEN_TILBAKEKREVING
            }
            beregnetPeriode.feilutbetaltBeløp == beregnetPeriode.tilbakekrevingsbeløpUtenRenter -> {
                KodeResultat.FULL_TILBAKEKREVING
            }
            else -> KodeResultat.DELVIS_TILBAKEKREVING
        }
    }

    private fun justerAvrunding(beregnetPeriode: Beregningsresultatsperiode,
                                perioder: List<Tilbakekrevingsperiode>): List<Tilbakekrevingsperiode> {
        val tilbakekrevingsbeløpUtenRenter = beregnetPeriode.tilbakekrevingsbeløpUtenRenter
        val totalTilbakekrevingsbeløp = beregnTotalTilbakekrevesbeløp(perioder)
        val differanse = totalTilbakekrevingsbeløp.subtract(tilbakekrevingsbeløpUtenRenter)

        return when {
            differanse.isGreaterThanZero() -> justerNed(differanse, perioder)
            differanse.isLessThanZero() -> justerOpp(differanse, perioder)
            else -> perioder
        }
    }

    private fun justerNed(differanse: BigDecimal, perioder: List<Tilbakekrevingsperiode>): List<Tilbakekrevingsperiode> {
        var diff = differanse
        return perioder.map { periode ->
            var justertebeløp = periode.beløp
            while (diff.isGreaterThanZero()) {
                justertebeløp = justertebeløp.map { beløp ->
                    if (Klassetype.FEIL == beløp.klassetype) {
                        beløp
                    } else {
                        diff = diff.subtract(BigDecimal.ONE)
                        beløp.copy(tilbakekrevesBeløp = beløp.tilbakekrevesBeløp.subtract(BigDecimal.ONE),
                                   uinnkrevdBeløp = beløp.uinnkrevdBeløp.add(BigDecimal.ONE))
                    }
                }
            }
            periode.copy(beløp = justertebeløp)

        }
    }

    private fun justerOpp(differanse: BigDecimal, perioder: List<Tilbakekrevingsperiode>): List<Tilbakekrevingsperiode> {
        var diff = differanse
        return perioder.map { periode ->
            var justertebeløp = periode.beløp
            while (diff.isLessThanZero()) {
                justertebeløp = justertebeløp.map { beløp ->
                    if (Klassetype.FEIL == beløp.klassetype) {
                        beløp
                    } else {
                        diff = diff.add(BigDecimal.ONE)
                        beløp.copy(tilbakekrevesBeløp = beløp.tilbakekrevesBeløp.add(BigDecimal.ONE),
                                   uinnkrevdBeløp = beløp.uinnkrevdBeløp.subtract(BigDecimal.ONE))
                    }
                }
            }
            periode.copy(beløp = justertebeløp)
        }
    }

    private fun oppdaterGjenståendeSkattetrekk(perioder: List<Tilbakekrevingsperiode>,
                                               kravgrunnlagsperioderMedSkatt: Map<Periode, BigDecimal>)
            : Map<Periode, BigDecimal> {
        val grunnlagsperioderMedSkatt = kravgrunnlagsperioderMedSkatt.toMutableMap()
        perioder.forEach {
            val skattBeløp = it.beløp
                    .filter { beløp -> Klassetype.YTEL == beløp.klassetype }
                    .sumOf { ytelsebeløp -> ytelsebeløp.skattBeløp }
            val gjenståendeSkattBeløp = kravgrunnlagsperioderMedSkatt.getNotNull(it.periode).subtract(skattBeløp)
            grunnlagsperioderMedSkatt[it.periode] = gjenståendeSkattBeløp
        }
        return grunnlagsperioderMedSkatt
    }


    private fun justerAvrundingSkatt(beregnetPeriode: Beregningsresultatsperiode,
                                     perioder: List<Tilbakekrevingsperiode>,
                                     kravgrunnlagsperioderMedSkatt: MutableMap<Periode, BigDecimal>)
            : List<Tilbakekrevingsperiode> {
        val totalSkattBeløp = perioder.sumOf { it.beløp.sumOf { beløp -> beløp.skattBeløp } }
        val beregnetSkattBeløp = beregnetPeriode.skattebeløp
        var differanse = totalSkattBeløp.subtract(beregnetSkattBeløp)

        return perioder.map {
            val periode = it.periode
            var justertebeløp = it.beløp
            justertebeløp = justertebeløp.map { beløp ->
                if (Klassetype.FEIL == beløp.klassetype) {
                    beløp
                } else {
                    val justerSkattOpp = differanse.isLessThanZero() &&
                                         kravgrunnlagsperioderMedSkatt.getNotNull(periode) >= BigDecimal.ONE
                    val justerSkattNed = differanse.isGreaterThanZero() &&
                                         beløp.skattBeløp >= BigDecimal.ONE
                    if (justerSkattOpp || justerSkattNed) {
                        val justering = BigDecimal(differanse.signum()).negate()
                        kravgrunnlagsperioderMedSkatt[periode] = kravgrunnlagsperioderMedSkatt.getNotNull(periode).add(differanse)
                        differanse = differanse.add(justering)
                        beløp.copy(skattBeløp = beløp.skattBeløp.add(BigDecimal.ONE))
                    } else {
                        beløp
                    }
                }
            }
            it.copy(beløp = justertebeløp)
        }
    }

    private fun beregnTotalTilbakekrevesbeløp(perioder: List<Tilbakekrevingsperiode>): BigDecimal {
        return perioder.sumOf { it.beløp.sumOf { beløp -> beløp.tilbakekrevesBeløp } }
    }

    private fun Map<Periode, BigDecimal>.getNotNull(key: Periode) = requireNotNull(this[key])
}

