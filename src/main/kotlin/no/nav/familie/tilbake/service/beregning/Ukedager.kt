package no.nav.familie.tilbake.service.beregning

import no.nav.familie.tilbake.common.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

object Ukedager {

    private const val DAGER_PR_UKE = 7
    private const val VIRKEDAGER_PR_UKE = 5
    private const val HELGEDAGER_PR_UKE = DAGER_PR_UKE - VIRKEDAGER_PR_UKE

    fun beregnAntallVirkedager(periode: Periode): Int {
        return beregnAntallVirkedager(periode.fom, periode.tom)
    }

    fun beregnAntallVirkedager(fom: LocalDate, tom: LocalDate): Int {
        require(!fom.isAfter(tom)) { "Utviklerfeil: fom $fom kan ikke være før tom $tom" }
        return try {
            // Utvid til nærmeste mandag tilbake i tid fra og med begynnelse (fom) (0-6 dager)
            val padBefore = fom.dayOfWeek.value - DayOfWeek.MONDAY.value
            // Utvid til nærmeste søndag fram i tid fra og med slutt (tom) (0-6 dager)
            val padAfter = DayOfWeek.SUNDAY.value - tom.dayOfWeek.value
            // Antall virkedager i perioden utvidet til hele uker
            val virkedagerPadded = Math.toIntExact(ChronoUnit.WEEKS.between(fom.minusDays(padBefore.toLong()),
                                                                            tom.plusDays(padAfter.toLong())
                                                                                    .plusDays(1)) * VIRKEDAGER_PR_UKE)
            // Antall virkedager i utvidelse
            val virkedagerPadding = min(padBefore, VIRKEDAGER_PR_UKE) + max(padAfter - HELGEDAGER_PR_UKE, 0)
            // Virkedager i perioden uten virkedagene fra utvidelse
            virkedagerPadded - virkedagerPadding
        } catch (e: ArithmeticException) {
            throw UnsupportedOperationException("Perioden er for lang til å beregne virkedager.", e)
        }
    }

    fun plusVirkedager(fom: LocalDate, virkedager: Int): LocalDate {
        val uker = virkedager / VIRKEDAGER_PR_UKE
        var dager = virkedager % VIRKEDAGER_PR_UKE
        var resultat = fom.plusWeeks(uker.toLong())
        while (dager > 0 || erHelg(resultat)) {
            if (!erHelg(resultat)) {
                dager--
            }
            resultat = resultat.plusDays(1)
        }
        return resultat
    }

    private fun erHelg(dato: LocalDate): Boolean {
        return dato.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}