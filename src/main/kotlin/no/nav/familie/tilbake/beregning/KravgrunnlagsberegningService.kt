package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.isNotZero
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.util.function.Function

@Service
object KravgrunnlagsberegningService {

    fun fordelKravgrunnlagBeløpPåPerioder(kravgrunnlag: Kravgrunnlag431,
                                          vurderingsperioder: List<Periode>): Map<Periode, FordeltKravgrunnlagsbeløp> {
        return vurderingsperioder.associateWith {
            FordeltKravgrunnlagsbeløp(beregnFeilutbetaltBeløp(kravgrunnlag, it),
                                      beregnUtbetaltYtelseBeløp(kravgrunnlag, it),
                                      beregnRiktigYtelseBeløp(kravgrunnlag, it))
        }
    }

    fun beregnFeilutbetaltBeløp(kravgrunnlag: Kravgrunnlag431, vurderingsperiode: Periode): BigDecimal {
        val feilutbetaltBeløpUtleder = { kgPeriode: Kravgrunnlagsperiode432 ->
            kgPeriode.beløp
                    .filter { it.klassetype == Klassetype.FEIL }
                    .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
        }
        return beregnBeløp(kravgrunnlag, vurderingsperiode, feilutbetaltBeløpUtleder)
    }

    fun validatePerioder(perioder: List<PeriodeDto>) {
        val perioderSomIkkeErHeleMåneder = perioder.filter {
            it.fom.dayOfMonth != 1 ||
            it.tom.dayOfMonth != YearMonth.from(it.tom).lengthOfMonth()
        }

        if (perioderSomIkkeErHeleMåneder.isNotEmpty()) {
            throw Feil(message = "Periode med ${perioderSomIkkeErHeleMåneder[0]} er ikke i hele måneder",
                       frontendFeilmelding = "Periode med ${perioderSomIkkeErHeleMåneder[0]} er ikke i hele måneder",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

    }

    /**
     * Utbetalt beløp er ikke justert med trekk, det er OK for vår bruk
     */
    private fun beregnUtbetaltYtelseBeløp(kravgrunnlag: Kravgrunnlag431, vurderingsperiode: Periode): BigDecimal {
        val feilutbetaltBeløpUtleder =
                { kgPeriode: Kravgrunnlagsperiode432 ->
                    kgPeriode.beløp
                            .filter { it.klassetype == Klassetype.YTEL }
                            .sumOf(Kravgrunnlagsbeløp433::opprinneligUtbetalingsbeløp)
                }
        return beregnBeløp(kravgrunnlag, vurderingsperiode, feilutbetaltBeløpUtleder)
    }

    /**
     * Riktig beløp er ikke justert med trekk, det er OK for vår bruk
     */
    private fun beregnRiktigYtelseBeløp(kravgrunnlag: Kravgrunnlag431, vurderingsperiode: Periode): BigDecimal {
        val feilutbetaltBeløpUtleder = { kgPeriode: Kravgrunnlagsperiode432 ->
            kgPeriode.beløp
                    .filter { it.klassetype == Klassetype.YTEL }
                    .sumOf(Kravgrunnlagsbeløp433::nyttBeløp)
        }
        return beregnBeløp(kravgrunnlag, vurderingsperiode, feilutbetaltBeløpUtleder)
    }

    private fun beregnBeløp(kravgrunnlag: Kravgrunnlag431,
                            vurderingsperiode: Periode,
                            beløpsummerer: Function<Kravgrunnlagsperiode432, BigDecimal>): BigDecimal {
        val sum = kravgrunnlag.perioder
                .sortedBy { it.periode.fom }
                .sumOf {
                    val beløp = beløpsummerer.apply(it)
                    if (beløp.isNotZero()) {
                        val beløpPerMåned: BigDecimal = BeløpsberegningUtil.beregnBeløpPerMåned(beløp, it.periode)
                        BeløpsberegningUtil.beregnBeløp(vurderingsperiode, it.periode, beløpPerMåned)
                    } else {
                        BigDecimal.ZERO
                    }
                }
        return sum.setScale(0, RoundingMode.HALF_UP)
    }

}
