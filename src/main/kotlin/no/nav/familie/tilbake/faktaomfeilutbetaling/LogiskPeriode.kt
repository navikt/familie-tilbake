package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.SortedMap

data class LogiskPeriode(val periode: Periode,
                         val feilutbetaltBeløp: BigDecimal) {

    val fom get() = periode.fom
    val tom get() = periode.tom
}

object LogiskPeriodeUtil {

    fun utledLogiskPeriode(feilutbetalingPrPeriode: SortedMap<Periode, BigDecimal>): List<LogiskPeriode> {
        var førsteDag: LocalDate? = null
        var sisteDag: LocalDate? = null
        var logiskPeriodeBeløp = BigDecimal.ZERO
        val resultat = mutableListOf<LogiskPeriode>()
        for ((periode, feilutbetaltBeløp) in feilutbetalingPrPeriode) {
            if (førsteDag == null && sisteDag == null) {
                førsteDag = periode.fom
                sisteDag = periode.tom
            } else {
                if (harUkedagerMellom(sisteDag!!, periode.fom)) {
                    resultat.add(LogiskPeriode(periode = Periode(førsteDag!!, sisteDag),
                                               feilutbetaltBeløp = logiskPeriodeBeløp))
                    førsteDag = periode.fom
                    logiskPeriodeBeløp = BigDecimal.ZERO
                }
                sisteDag = periode.tom
            }
            logiskPeriodeBeløp = logiskPeriodeBeløp.add(feilutbetaltBeløp)
        }
        if (BigDecimal.ZERO.compareTo(logiskPeriodeBeløp) != 0) {
            resultat.add(LogiskPeriode(periode = Periode(førsteDag!!, sisteDag!!),
                                       feilutbetaltBeløp = logiskPeriodeBeløp))
        }
        return resultat.toList()
    }

    private fun harUkedagerMellom(dag1: LocalDate, dag2: LocalDate): Boolean {
        require(dag2 > dag1) { "dag2 må være etter dag1" }
        if (dag1.plusDays(1) == dag2) {
            return false
        }
        if (dag1.plusDays(2) == dag2 && (dag1.dayOfWeek == DayOfWeek.FRIDAY || dag1.dayOfWeek == DayOfWeek.SATURDAY)) {
            return false
        }
        return !(dag1.plusDays(3) == dag2 && dag1.dayOfWeek == DayOfWeek.FRIDAY)
    }
}
