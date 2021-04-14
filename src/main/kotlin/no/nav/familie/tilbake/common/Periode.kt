package no.nav.familie.tilbake.common

import java.time.LocalDate
import java.time.YearMonth

data class Periode(val fom: YearMonth,
                   val tom: YearMonth) : Comparable<Periode> {

    constructor(fom: LocalDate, tom: LocalDate) : this(YearMonth.from(fom), YearMonth.from(tom))

    init {
        require(tom >= fom) { "Til-og-med-måned før fra-og-med-måned: $fom > $tom" }
    }

    private fun overlapper(dato: YearMonth): Boolean {
        return dato in fom..tom
    }

    fun overlapper(other: Periode): Boolean {
        return overlapper(other.fom) || overlapper(other.tom) || other.overlapper(fom)
    }

    fun snitt(annen: Periode): Periode? {
        return if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            Periode(maxOf(fom, annen.fom),
                    minOf(tom, annen.tom))
        }
    }

    fun omslutter(annen: Periode): Boolean {
        return annen.fom >= fom && annen.tom <= tom
    }

    fun lengdeIMåneder(): Long {
        return (tom.year * 12 + tom.monthValue) - (fom.year * 12 + fom.monthValue) + 1L
    }

    companion object {

        val COMPARATOR: Comparator<Periode> = Comparator.comparing(Periode::fom).thenComparing(Periode::tom)
    }

    override fun compareTo(other: Periode): Int {
        return COMPARATOR.compare(this, other)
    }

}
