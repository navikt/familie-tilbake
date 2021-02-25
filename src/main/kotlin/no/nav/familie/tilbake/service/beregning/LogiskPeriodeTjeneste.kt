package no.nav.familie.tilbake.service.beregning

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.service.modell.LogiskPeriode
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.ArrayList
import java.util.SortedMap

object LogiskPeriodeTjeneste {

    fun utledLogiskPeriode(feilutbetalingPrPeriode: SortedMap<Periode, BigDecimal>): List<LogiskPeriode> {
        var førsteDag: LocalDate = LocalDate.MIN
        var sisteDag: LocalDate = LocalDate.MIN
        var logiskPeriodeBeløp = BigDecimal.ZERO
        val resultat: MutableList<LogiskPeriode> = ArrayList<LogiskPeriode>()
        feilutbetalingPrPeriode.forEach {(periode, feilutbetaltBeløp) ->
            if (førsteDag == LocalDate.MIN && sisteDag == LocalDate.MIN) {
                førsteDag = periode.fom
                sisteDag = periode.tom
            } else {
                if (harUkedagerMellom(sisteDag, periode.fom)) {
                    resultat.add(LogiskPeriode(Periode(førsteDag, sisteDag), logiskPeriodeBeløp))
                    førsteDag = periode.fom
                    logiskPeriodeBeløp = BigDecimal.ZERO
                }
                sisteDag = periode.tom
            }
            logiskPeriodeBeløp = logiskPeriodeBeløp.add(feilutbetaltBeløp)
        }
        if (BigDecimal.ZERO.compareTo(logiskPeriodeBeløp) != 0) {
            resultat.add(LogiskPeriode(Periode(førsteDag, sisteDag), logiskPeriodeBeløp))
        }
        return resultat
    }

    private fun harUkedagerMellom(dag1: LocalDate, dag2: LocalDate): Boolean {
        require(dag2.isAfter(dag1)) { "dag2 må være etter dag1" }
        if (dag1.plusDays(1) == dag2) {
            return false
        }
        if (dag1.plusDays(2) == dag2 && (dag1.dayOfWeek == DayOfWeek.FRIDAY || dag1.dayOfWeek == DayOfWeek.SATURDAY)) {
            return false
        }
        return !(dag1.plusDays(3) == dag2 && dag1.dayOfWeek == DayOfWeek.FRIDAY)
    }
}